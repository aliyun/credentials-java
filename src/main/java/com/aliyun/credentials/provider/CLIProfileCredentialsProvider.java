package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.utils.ProviderName;
import com.aliyun.credentials.utils.StringUtils;
import com.aliyun.tea.utils.Validate;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

public class CLIProfileCredentialsProvider implements AlibabaCloudCredentialsProvider {
    private final String CLI_CREDENTIALS_CONFIG_PATH = System.getProperty("user.home") +
            "/.aliyun/config.json";
    private volatile AlibabaCloudCredentialsProvider credentialsProvider;
    private volatile String currentProfileName;
    private final Object credentialsProviderLock = new Object();

    private CLIProfileCredentialsProvider(Builder builder) {
        this.currentProfileName = builder.profileName == null ? System.getenv("ALIBABA_CLOUD_PROFILE") : builder.profileName;
    }

    public static Builder builder() {
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
        CredentialModel credential = this.credentialsProvider.getCredentials();
        return CredentialModel.builder()
                .accessKeyId(credential.getAccessKeyId())
                .accessKeySecret(credential.getAccessKeySecret())
                .securityToken(credential.getSecurityToken())
                .expiration(credential.getExpiration())
                .type(credential.getType())
                .providerName(String.format("%s/%s", this.getProviderName(), credential.getProviderName()))
                .build();
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
                                            .providerName(ProviderName.STATIC_AK)
                                            .build())
                                    .build();
                        case "StsToken":
                            return StaticCredentialsProvider.builder()
                                    .credential(CredentialModel.builder()
                                            .accessKeyId(Validate.notNull(
                                                    profile.getAccessKeyId(), "AccessKeyId must not be null."))
                                            .accessKeySecret(Validate.notNull(
                                                    profile.getAccessKeySecret(), "AccessKeySecret must not be null."))
                                            .securityToken(Validate.notNull(
                                                    profile.getSecurityToken(), "SecurityToken must not be null."
                                            ))
                                            .type(AuthConstant.STS)
                                            .providerName(ProviderName.STATIC_STS)
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
                                            .providerName(ProviderName.STATIC_AK)
                                            .build())
                                    .build();
                            ;
                            return RamRoleArnCredentialProvider.builder()
                                    .credentialsProvider(innerProvider)
                                    .durationSeconds(profile.getDurationSeconds())
                                    .roleArn(profile.getRoleArn())
                                    .roleSessionName(profile.getRoleSessionName())
                                    .stsRegionId(profile.getStsRegionId())
                                    .enableVpc(profile.getEnableVpc())
                                    .policy(profile.getPolicy())
                                    .externalId(profile.getExternalId())
                                    .build();
                        case "EcsRamRole":
                            return EcsRamRoleCredentialProvider.builder()
                                    .roleName(profile.getRamRoleName())
                                    .build();
                        case "OIDC":
                            return OIDCRoleArnCredentialProvider.builder()
                                    .durationSeconds(profile.getDurationSeconds())
                                    .roleArn(profile.getRoleArn())
                                    .roleSessionName(profile.getRoleSessionName())
                                    .oidcProviderArn(profile.getOidcProviderArn())
                                    .oidcTokenFilePath(profile.getOidcTokenFile())
                                    .stsRegionId(profile.getStsRegionId())
                                    .enableVpc(profile.getEnableVpc())
                                    .policy(profile.getPolicy())
                                    .build();
                        case "ChainableRamRoleArn":
                            AlibabaCloudCredentialsProvider previousProvider = reloadCredentialsProvider(config, profile.getSourceProfile());
                            return RamRoleArnCredentialProvider.builder()
                                    .credentialsProvider(previousProvider)
                                    .durationSeconds(profile.getDurationSeconds())
                                    .roleArn(profile.getRoleArn())
                                    .roleSessionName(profile.getRoleSessionName())
                                    .stsRegionId(profile.getStsRegionId())
                                    .enableVpc(profile.getEnableVpc())
                                    .policy(profile.getPolicy())
                                    .externalId(profile.getExternalId())
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

    @Override
    public String getProviderName() {
        return ProviderName.CLI_PROFILE;
    }

    @Override
    public void close() {
    }

    public static final class Builder {
        private String profileName;

        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public CLIProfileCredentialsProvider build() {
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
        @SerializedName("sts_token")
        private String securityToken;
        @SerializedName("ram_role_arn")
        private String roleArn;
        @SerializedName("ram_session_name")
        private String roleSessionName;
        @SerializedName("expired_seconds")
        private Integer durationSeconds;
        @SerializedName("sts_region")
        private String stsRegionId;
        @SerializedName("enable_vpc")
        private Boolean enableVpc;
        @SerializedName("ram_role_name")
        private String ramRoleName;
        @SerializedName("oidc_token_file")
        private String oidcTokenFile;
        @SerializedName("oidc_provider_arn")
        private String oidcProviderArn;
        @SerializedName("source_profile")
        private String sourceProfile;
        @SerializedName("policy")
        private String policy;
        @SerializedName("external_id")
        private String externalId;

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

        public String getSecurityToken() {
            return securityToken;
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

        public Boolean getEnableVpc() {
            return enableVpc;
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

        public String getPolicy() {
            return policy;
        }

        public String getExternalId() {
            return externalId;
        }
    }
}
