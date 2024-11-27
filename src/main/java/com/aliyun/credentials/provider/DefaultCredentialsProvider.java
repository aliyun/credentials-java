package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.utils.ProviderName;
import com.aliyun.credentials.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class DefaultCredentialsProvider implements AlibabaCloudCredentialsProvider {
    private final List<AlibabaCloudCredentialsProvider> defaultProviders = new ArrayList<AlibabaCloudCredentialsProvider>();
    private static final List<AlibabaCloudCredentialsProvider> USER_CONFIGURATION_PROVIDERS = new Vector<AlibabaCloudCredentialsProvider>();
    private volatile AlibabaCloudCredentialsProvider lastUsedCredentialsProvider;
    private final Boolean reuseLastProviderEnabled;

    public DefaultCredentialsProvider() {
        this.reuseLastProviderEnabled = true;
        createDefaultChain();
    }

    private DefaultCredentialsProvider(Builder builder) {
        this.reuseLastProviderEnabled = builder.reuseLastProviderEnabled;
        createDefaultChain();
    }

    private void createDefaultChain() {
        defaultProviders.add(new SystemPropertiesCredentialsProvider());
        defaultProviders.add(new EnvironmentVariableCredentialsProvider());
        if (AuthUtils.environmentEnableOIDC()) {
            defaultProviders.add(OIDCRoleArnCredentialProvider.builder().build());
        }
        defaultProviders.add(CLIProfileCredentialsProvider.builder().build());
        defaultProviders.add(new ProfileCredentialsProvider());
        if (!AuthUtils.isDisableECSMetaData()) {
            defaultProviders.add(EcsRamRoleCredentialProvider.builder().build());
        }
        String uri = AuthUtils.getEnvironmentCredentialsURI();
        if (!StringUtils.isEmpty(uri)) {
            defaultProviders.add(URLCredentialProvider.builder()
                    .credentialsURI(uri)
                    .build());
        }
    }


    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CredentialModel getCredentials() {
        if (this.reuseLastProviderEnabled && this.lastUsedCredentialsProvider != null) {
            return this.lastUsedCredentialsProvider.getCredentials();
        }
        CredentialModel credential;
        List<String> errorMessages = new ArrayList<>();
        if (USER_CONFIGURATION_PROVIDERS.size() > 0) {
            for (AlibabaCloudCredentialsProvider provider : USER_CONFIGURATION_PROVIDERS) {
                try {
                    credential = provider.getCredentials();
                    if (credential != null) {
                        this.lastUsedCredentialsProvider = provider;
                        return CredentialModel.builder()
                                .accessKeyId(credential.getAccessKeyId())
                                .accessKeySecret(credential.getAccessKeySecret())
                                .securityToken(credential.getSecurityToken())
                                .expiration(credential.getExpiration())
                                .type(credential.getType())
                                .providerName(String.format("%s/%s", this.getProviderName(), credential.getProviderName()))
                                .build();
                    }
                } catch (Exception e) {
                    errorMessages.add(provider.getClass().getName() + ": " + e.getMessage());
                }
            }
        }
        for (AlibabaCloudCredentialsProvider provider : defaultProviders) {
            try {
                credential = provider.getCredentials();
                if (credential != null) {
                    this.lastUsedCredentialsProvider = provider;
                    return CredentialModel.builder()
                            .accessKeyId(credential.getAccessKeyId())
                            .accessKeySecret(credential.getAccessKeySecret())
                            .securityToken(credential.getSecurityToken())
                            .expiration(credential.getExpiration())
                            .type(credential.getType())
                            .providerName(String.format("%s/%s", this.getProviderName(), credential.getProviderName()))
                            .build();
                }
            } catch (Exception e) {
                errorMessages.add(provider.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        throw new CredentialException("Unable to load credentials from any of the providers in the chain: " + errorMessages);
    }

    @Deprecated
    public static boolean addCredentialsProvider(AlibabaCloudCredentialsProvider provider) {
        return DefaultCredentialsProvider.USER_CONFIGURATION_PROVIDERS.add(provider);
    }

    @Deprecated
    public static boolean removeCredentialsProvider(AlibabaCloudCredentialsProvider provider) {
        return DefaultCredentialsProvider.USER_CONFIGURATION_PROVIDERS.remove(provider);
    }

    @Deprecated
    public static boolean containsCredentialsProvider(AlibabaCloudCredentialsProvider provider) {
        return DefaultCredentialsProvider.USER_CONFIGURATION_PROVIDERS.contains(provider);
    }

    @Deprecated
    public static void clearCredentialsProvider() {
        DefaultCredentialsProvider.USER_CONFIGURATION_PROVIDERS.clear();
    }

    @Override
    public String getProviderName() {
        return ProviderName.DEFAULT;
    }

    @Override
    public void close() {
    }

    public static final class Builder {
        private Boolean reuseLastProviderEnabled = true;

        public Builder reuseLastProviderEnabled(Boolean reuseLastProviderEnabled) {
            this.reuseLastProviderEnabled = reuseLastProviderEnabled;
            return this;
        }

        public DefaultCredentialsProvider build() {
            return new DefaultCredentialsProvider(this);
        }
    }

}
