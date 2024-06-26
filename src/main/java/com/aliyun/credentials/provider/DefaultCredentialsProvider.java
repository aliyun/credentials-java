package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class DefaultCredentialsProvider implements AlibabaCloudCredentialsProvider {
    private List<AlibabaCloudCredentialsProvider> defaultProviders = new ArrayList<AlibabaCloudCredentialsProvider>();
    private static final List<AlibabaCloudCredentialsProvider> USER_CONFIGURATION_PROVIDERS =
            new Vector<AlibabaCloudCredentialsProvider>();

    public DefaultCredentialsProvider() {
        defaultProviders.add(new SystemPropertiesCredentialsProvider());
        defaultProviders.add(new EnvironmentVariableCredentialsProvider());
        if (AuthUtils.environmentEnableOIDC()) {
            defaultProviders.add(OIDCRoleArnCredentialProvider.builder()
                    .roleArn(AuthUtils.getEnvironmentRoleArn())
                    .oidcProviderArn(AuthUtils.getEnvironmentOIDCProviderArn())
                    .oidcTokenFilePath(AuthUtils.getEnvironmentOIDCTokenFilePath())
                    .build());
        }
        defaultProviders.add(new ProfileCredentialsProvider());
        String roleName = AuthUtils.getEnvironmentECSMetaData();
        if (null != roleName) {
            defaultProviders.add(EcsRamRoleCredentialProvider.builder()
                    .roleName(roleName)
                    .build());
        }
    }

    @Override
    public CredentialModel getCredentials() {
        CredentialModel credential;
        if (USER_CONFIGURATION_PROVIDERS.size() > 0) {
            for (AlibabaCloudCredentialsProvider provider : USER_CONFIGURATION_PROVIDERS) {
                credential = provider.getCredentials();
                if (null != credential) {
                    return credential;
                }
            }
        }
        for (AlibabaCloudCredentialsProvider provider : defaultProviders) {
            credential = provider.getCredentials();
            if (null != credential) {
                return credential;
            }
        }
        throw new CredentialException("not found credentials");
    }

    public static boolean addCredentialsProvider(AlibabaCloudCredentialsProvider provider) {
        return DefaultCredentialsProvider.USER_CONFIGURATION_PROVIDERS.add(provider);
    }

    public static boolean removeCredentialsProvider(AlibabaCloudCredentialsProvider provider) {
        return DefaultCredentialsProvider.USER_CONFIGURATION_PROVIDERS.remove(provider);
    }

    public static boolean containsCredentialsProvider(AlibabaCloudCredentialsProvider provider) {
        return DefaultCredentialsProvider.USER_CONFIGURATION_PROVIDERS.contains(provider);
    }

    public static void clearCredentialsProvider() {
        DefaultCredentialsProvider.USER_CONFIGURATION_PROVIDERS.clear();
    }
}
