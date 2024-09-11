package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.utils.StringUtils;
import com.aliyun.tea.utils.Validate;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
            defaultProviders.add(OIDCRoleArnCredentialProvider.builder()
                    .roleArn(AuthUtils.getEnvironmentRoleArn())
                    .oidcProviderArn(AuthUtils.getEnvironmentOIDCProviderArn())
                    .oidcTokenFilePath(AuthUtils.getEnvironmentOIDCTokenFilePath())
                    .build());
        }
        defaultProviders.add(CLIProfileCredentialsProvider.builder().build());
        defaultProviders.add(new ProfileCredentialsProvider());
        String roleName = AuthUtils.getEnvironmentECSMetaData();
        if (null != roleName) {
            defaultProviders.add(EcsRamRoleCredentialProvider.builder()
                    .roleName(roleName)
                    .build());
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
                    this.lastUsedCredentialsProvider = provider;
                    return credential;
                } catch (Exception e) {
                    errorMessages.add(provider.getClass().getName() + ": " + e.getMessage());
                }
            }
        }
        for (AlibabaCloudCredentialsProvider provider : defaultProviders) {
            try {
                credential = provider.getCredentials();
                this.lastUsedCredentialsProvider = provider;
                return credential;
            } catch (Exception e) {
                errorMessages.add(provider.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        throw new CredentialException("Unable to load credentials from any of the providers in the chain: ." + errorMessages);
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

    public static final class Builder {
        private Boolean reuseLastProviderEnabled = true;

        public Builder reuseLastProviderEnabled(Boolean reuseLastProviderEnabled) {
            this.reuseLastProviderEnabled = reuseLastProviderEnabled;
            return this;
        }

        DefaultCredentialsProvider build() {
            return new DefaultCredentialsProvider(this);
        }
    }

}

/**
 * CLIProfileCredentialsProvider is not public.
 */
class CLIProfileCredentialsProvider implements AlibabaCloudCredentialsProvider {
    private final String CLI_CREDENTIALS_CONFIG_PATH = System.getProperty("user.home") +
            "/.aliyun/config.json";
    private volatile AlibabaCloudCredentialsProvider credentialsProvider;
    private volatile String currentProfileName;
    private final Object credentialsProviderLock = new Object();

    private CLIProfileCredentialsProvider(Builder builder) {
        this.currentProfileName = builder.profileName;
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    public CredentialModel getCredentials() {
        if (AuthUtils.isDisableCLIProfile()) {
            throw new CredentialException("CLI credentials file is disabled.");
        }
        Config config = parseProfile(CLI_CREDENTIALS_CONFIG_PATH);
        if (null == config) {
            throw new CredentialException("Unable to get profile from empty CLI credentials file.");
        }
        String refreshedProfileName = System.getenv("ALIBABA_CLOUD_PROFILE");
        if (shouldReloadCredentialsProvider(refreshedProfileName)) {
            synchronized (credentialsProviderLock) {
                if (shouldReloadCredentialsProvider(refreshedProfileName)) {
                    if (!StringUtils.isEmpty(refreshedProfileName)) {
                        this.currentProfileName = refreshedProfileName;
                    }
                    this.credentialsProvider = reloadCredentialsProvider(config, this.currentProfileName);
                }
            }
        }
        return this.credentialsProvider.getCredentials();
    }

    AlibabaCloudCredentialsProvider reloadCredentialsProvider(Config config, String profileName) {
        String currentProfileName = !StringUtils.isEmpty(profileName) ? profileName : config.getCurrent();
        List<Profile> profiles = config.getProfiles();
        if (profiles != null && !profiles.isEmpty()) {
            for (Profile profile : profiles) {
                if (!StringUtils.isEmpty(profile.getName()) && profile.getName().equals(currentProfileName)) {
                    switch (profile.getMode()) {
                        case "AK":
                            return StaticCredentialsProvider.builder()
                                    .credential(CredentialModel.builder()
                                            .accessKeyId(Validate.notNull(
                                                    profile.getAccessKeyId(), "AccessKeyId must not be null."))
                                            .accessKeySecret(Validate.notNull(
                                                    profile.getAccessKeySecret(), "AccessKeySecret must not be null."))
                                            .type(AuthConstant.ACCESS_KEY)
                                            .build())
                                    .build();
                        case "RamRoleArn":
                            AlibabaCloudCredentialsProvider innerProvider = StaticCredentialsProvider.builder()
                                    .credential(CredentialModel.builder()
                                            .accessKeyId(Validate.notNull(
                                                    profile.getAccessKeyId(), "AccessKeyId must not be null."))
                                            .accessKeySecret(Validate.notNull(
                                                    profile.getAccessKeySecret(), "AccessKeySecret must not be null."))
                                            .type(AuthConstant.ACCESS_KEY)
                                            .build())
                                    .build();
                            ;
                            return RamRoleArnCredentialProvider.builder()
                                    .credentialsProvider(innerProvider)
                                    .durationSeconds(profile.getDurationSeconds() != null ? profile.getDurationSeconds() : 3600)
                                    .roleArn(profile.getRoleArn())
                                    .roleSessionName(profile.getRoleSessionName())
                                    .build();
                        case "EcsRamRole":
                            return EcsRamRoleCredentialProvider.builder()
                                    .roleName(profile.getRamRoleName())
                                    .build();
                        case "OIDC":
                            return OIDCRoleArnCredentialProvider.builder()
                                    .durationSeconds(profile.getDurationSeconds() != null ? profile.getDurationSeconds() : 3600)
                                    .roleArn(profile.getRoleArn())
                                    .roleSessionName(profile.getRoleSessionName())
                                    .oidcProviderArn(profile.getOidcProviderArn())
                                    .oidcTokenFilePath(profile.getOidcTokenFile())
                                    .build();
                        case "ChainableRamRoleArn":
                            AlibabaCloudCredentialsProvider previousProvider = reloadCredentialsProvider(config, profile.getSourceProfile());
                            return RamRoleArnCredentialProvider.builder()
                                    .credentialsProvider(previousProvider)
                                    .durationSeconds(profile.getDurationSeconds() != null ? profile.getDurationSeconds() : 3600)
                                    .roleArn(profile.getRoleArn())
                                    .roleSessionName(profile.getRoleSessionName())
                                    .build();
                        default:
                            throw new CredentialException(String.format("Unsupported profile mode '%s' form CLI credentials file.", profile.getMode()));
                    }
                }
            }
        }
        throw new CredentialException(String.format("Unable to get profile with '%s' form CLI credentials file.", currentProfileName));
    }

    Config parseProfile(String configFilePath) {
        File configFile = new File(configFilePath);
        if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
            throw new CredentialException(String.format("Unable to open credentials file: %s.", configFile.getAbsolutePath()));
        }
        Gson gson = new Gson();
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            String jsonContent = sb.toString();
            return gson.fromJson(jsonContent, Config.class);
        } catch (Exception e) {
            throw new CredentialException(String.format("Failed to parse credential form CLI credentials file: %s.", configFile.getAbsolutePath()));
        }
    }

    boolean shouldReloadCredentialsProvider(String profileName) {
        return this.credentialsProvider == null || (!StringUtils.isEmpty(this.currentProfileName) && !StringUtils.isEmpty(profileName) && !this.currentProfileName.equals(profileName));
    }

    String getProfileName() {
        return this.currentProfileName;
    }

    static final class Builder {
        private String profileName = System.getenv("ALIBABA_CLOUD_PROFILE");

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        CLIProfileCredentialsProvider build() {
            return new CLIProfileCredentialsProvider(this);
        }
    }

    static class Config {
        @SerializedName("current")
        private String current;
        @SerializedName("profiles")
        private List<Profile> profiles;

        public String getCurrent() {
            return current;
        }

        public List<Profile> getProfiles() {
            return profiles;
        }
    }

    static class Profile {
        @SerializedName("name")
        private String name;
        @SerializedName("mode")
        private String mode;
        @SerializedName("access_key_id")
        private String accessKeyId;
        @SerializedName("access_key_secret")
        private String accessKeySecret;
        @SerializedName("ram_role_arn")
        private String roleArn;
        @SerializedName("ram_session_name")
        private String roleSessionName;
        @SerializedName("expired_seconds")
        private Integer durationSeconds;
        @SerializedName("sts_region")
        private String stsRegionId;
        @SerializedName("ram_role_name")
        private String ramRoleName;
        @SerializedName("oidc_token_file")
        private String oidcTokenFile;
        @SerializedName("oidc_provider_arn")
        private String oidcProviderArn;
        @SerializedName("source_profile")
        private String sourceProfile;

        public String getName() {
            return name;
        }

        public String getMode() {
            return mode;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public String getRoleArn() {
            return roleArn;
        }

        public String getRoleSessionName() {
            return roleSessionName;
        }

        public Integer getDurationSeconds() {
            return durationSeconds;
        }

        public String getStsRegionId() {
            return stsRegionId;
        }

        public String getRamRoleName() {
            return ramRoleName;
        }

        public String getOidcTokenFile() {
            return oidcTokenFile;
        }

        public String getOidcProviderArn() {
            return oidcProviderArn;
        }

        public String getSourceProfile() {
            return sourceProfile;
        }
    }
}
