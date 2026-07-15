package com.aliyun.credentials.provider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.utils.ProviderName;
import com.aliyun.credentials.utils.StringUtils;
import com.aliyun.tea.utils.Validate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class CLIProfileCredentialsProvider implements AlibabaCloudCredentialsProvider {
    private static final Map<String, String> OAUTH_BASE_URL_MAP = new HashMap<String, String>() {{
        put("CN", "https://oauth.aliyun.com");
        put("INTL", "https://oauth.alibabacloud.com");
    }};
    private static final Map<String, String> OAUTH_CLIENT_MAP = new HashMap<String, String>() {{
        put("CN", "4038181954557748008");
        put("INTL", "4103531455503354461");
    }};

    private final String CLI_CREDENTIALS_CONFIG_PATH = new File(
            new File(System.getProperty("user.home"), ".aliyun"),
            "config.json"
    ).getPath();
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
        String selectedProfileName = !StringUtils.isEmpty(profileName) ? profileName : config.getCurrent();
        List<Profile> profiles = config.getProfiles();
        if (profiles != null && !profiles.isEmpty()) {
            for (Profile profile : profiles) {
                if (!StringUtils.isEmpty(profile.getName()) && profile.getName().equals(selectedProfileName)) {
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
                        case "CloudSSO":
                            return CloudSSOCredentialsProvider.builder()
                                    .signInUrl(profile.getSignInUrl())
                                    .accountId(profile.getAccountId())
                                    .accessConfig(profile.getAccessConfig())
                                    .accessToken(profile.getAccessToken())
                                    .accessTokenExpire(profile.getAccessTokenExpire())
                                    .build();
                        case "OAuth":
                            String siteType = profile.getOauthSiteType() != null
                                    ? profile.getOauthSiteType().toUpperCase() : "";
                            String oauthSignInUrl = OAUTH_BASE_URL_MAP.get(siteType);
                            if (StringUtils.isEmpty(oauthSignInUrl)) {
                                throw new CredentialException("Invalid OAuth site type, support CN or INTL.");
                            }
                            String oauthClientId = OAUTH_CLIENT_MAP.get(siteType);
                            return OAuthCredentialsProvider.builder()
                                    .signInUrl(oauthSignInUrl)
                                    .clientId(oauthClientId)
                                    .refreshToken(profile.getOauthRefreshToken())
                                    .accessToken(profile.getOauthAccessToken())
                                    .accessTokenExpire(profile.getOauthAccessTokenExpire())
                                    .tokenUpdateCallback(createOAuthTokenUpdateCallback())
                                    .build();
                        case "External":
                            return ExternalCredentialsProvider.builder()
                                    .processCommand(profile.getProcessCommand())
                                    .credentialUpdateCallback(createExternalCredentialUpdateCallback())
                                    .build();
                        default:
                            throw new CredentialException(String.format("Unsupported profile mode '%s' form CLI credentials file.", profile.getMode()));
                    }
                }
            }
        }
        throw new CredentialException(String.format("Unable to get profile with '%s' form CLI credentials file.", selectedProfileName));
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

    private OAuthCredentialsProvider.OAuthTokenUpdateCallback createOAuthTokenUpdateCallback() {
        return (refreshToken, accessToken, accessKeyId, accessKeySecret, securityToken, accessTokenExpire, stsExpire) -> {
            updateOAuthTokens(refreshToken, accessToken, accessKeyId, accessKeySecret, securityToken, accessTokenExpire, stsExpire);
        };
    }

    private ExternalCredentialsProvider.ExternalCredentialUpdateCallback createExternalCredentialUpdateCallback() {
        return (accessKeyId, accessKeySecret, securityToken, expiration) -> {
            updateExternalCredentials(accessKeyId, accessKeySecret, securityToken, expiration);
        };
    }

    private void updateOAuthTokens(String refreshToken, String accessToken, String accessKeyId,
                                   String accessKeySecret, String securityToken,
                                   long accessTokenExpire, long stsExpire) {
        File configFile = new File(CLI_CREDENTIALS_CONFIG_PATH);
        if (!configFile.exists()) {
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(configFile, "rw");
             FileChannel channel = raf.getChannel();
             FileLock lock = channel.lock()) {
            if (!lock.isValid()) {
                return;
            }

            byte[] bytes = new byte[(int) raf.length()];
            raf.readFully(bytes);
            String jsonContent = new String(bytes, "UTF-8");

            Gson gson = new Gson();
            Config config = gson.fromJson(jsonContent, Config.class);
            if (config == null || config.getProfiles() == null) {
                return;
            }

            String profileName = this.currentProfileName;
            if (StringUtils.isEmpty(profileName)) {
                profileName = config.getCurrent();
            }

            Profile oauthProfile = findOAuthProfile(config, profileName);
            if (oauthProfile == null) {
                return;
            }

            oauthProfile.setOauthRefreshToken(refreshToken);
            oauthProfile.setOauthAccessToken(accessToken);
            oauthProfile.setOauthAccessTokenExpire(accessTokenExpire);
            oauthProfile.setAccessKeyId(accessKeyId);
            oauthProfile.setAccessKeySecret(accessKeySecret);
            oauthProfile.setSecurityToken(securityToken);
            oauthProfile.setStsExpire(stsExpire);

            Gson writer = new GsonBuilder().setPrettyPrinting().create();
            String updatedJson = writer.toJson(config);

            raf.seek(0);
            raf.setLength(0);
            raf.write(updatedJson.getBytes("UTF-8"));
        } catch (Exception e) {
            // Warning only
        }
    }

    private void updateExternalCredentials(String accessKeyId, String accessKeySecret,
                                           String securityToken, long expiration) {
        File configFile = new File(CLI_CREDENTIALS_CONFIG_PATH);
        if (!configFile.exists()) {
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(configFile, "rw");
             FileChannel channel = raf.getChannel();
             FileLock lock = channel.lock()) {
            if (!lock.isValid()) {
                return;
            }

            byte[] bytes = new byte[(int) raf.length()];
            raf.readFully(bytes);
            String jsonContent = new String(bytes, "UTF-8");

            Gson gson = new Gson();
            Config config = gson.fromJson(jsonContent, Config.class);
            if (config == null || config.getProfiles() == null) {
                return;
            }

            String profileName = this.currentProfileName;
            if (StringUtils.isEmpty(profileName)) {
                profileName = config.getCurrent();
            }

            Profile externalProfile = findExternalProfile(config, profileName);
            if (externalProfile == null) {
                return;
            }

            externalProfile.setAccessKeyId(accessKeyId);
            externalProfile.setAccessKeySecret(accessKeySecret);
            externalProfile.setSecurityToken(securityToken);
            externalProfile.setStsExpire(expiration);

            Gson writer = new GsonBuilder().setPrettyPrinting().create();
            String updatedJson = writer.toJson(config);

            raf.seek(0);
            raf.setLength(0);
            raf.write(updatedJson.getBytes("UTF-8"));
        } catch (Exception e) {
            // Warning only
        }
    }

    private Profile findOAuthProfile(Config config, String profileName) {
        if (config.getProfiles() == null) {
            return null;
        }
        for (Profile p : config.getProfiles()) {
            if (p.getName() != null && p.getName().equals(profileName)) {
                if ("OAuth".equals(p.getMode())) {
                    return p;
                }
                if (!StringUtils.isEmpty(p.getSourceProfile())) {
                    return findOAuthProfile(config, p.getSourceProfile());
                }
                return null;
            }
        }
        return null;
    }

    private Profile findExternalProfile(Config config, String profileName) {
        if (config.getProfiles() == null) {
            return null;
        }
        for (Profile p : config.getProfiles()) {
            if (p.getName() != null && p.getName().equals(profileName)) {
                if ("External".equals(p.getMode())) {
                    return p;
                }
                if (!StringUtils.isEmpty(p.getSourceProfile())) {
                    return findExternalProfile(config, p.getSourceProfile());
                }
                return null;
            }
        }
        return null;
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
        @SerializedName("cloud_sso_sign_in_url")
        private String signInUrl;
        @SerializedName("cloud_sso_account_id")
        private String accountId;
        @SerializedName("cloud_sso_access_config")
        private String accessConfig;
        @SerializedName("access_token")
        private String accessToken;
        @SerializedName("cloud_sso_access_token_expire")
        private Long accessTokenExpire;
        @SerializedName("oauth_site_type")
        private String oauthSiteType;
        @SerializedName("oauth_refresh_token")
        private String oauthRefreshToken;
        @SerializedName("oauth_access_token")
        private String oauthAccessToken;
        @SerializedName("oauth_access_token_expire")
        private Long oauthAccessTokenExpire;
        @SerializedName("process_command")
        private String processCommand;
        @SerializedName("sts_expiration")
        private Long stsExpire;

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

        public String getSignInUrl() {
            return signInUrl;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getAccessConfig() {
            return accessConfig;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public long getAccessTokenExpire() {
            if (accessTokenExpire == null) {
                return 0L;
            }
            return accessTokenExpire;
        }

        public String getOauthSiteType() {
            return oauthSiteType;
        }

        public String getOauthRefreshToken() {
            return oauthRefreshToken;
        }

        public String getOauthAccessToken() {
            return oauthAccessToken;
        }

        public long getOauthAccessTokenExpire() {
            if (oauthAccessTokenExpire == null) {
                return 0L;
            }
            return oauthAccessTokenExpire;
        }

        public String getProcessCommand() {
            return processCommand;
        }

        public long getStsExpire() {
            if (stsExpire == null) {
                return 0L;
            }
            return stsExpire;
        }

        public void setOauthRefreshToken(String oauthRefreshToken) {
            this.oauthRefreshToken = oauthRefreshToken;
        }

        public void setOauthAccessToken(String oauthAccessToken) {
            this.oauthAccessToken = oauthAccessToken;
        }

        public void setOauthAccessTokenExpire(long oauthAccessTokenExpire) {
            this.oauthAccessTokenExpire = oauthAccessTokenExpire;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public void setSecurityToken(String securityToken) {
            this.securityToken = securityToken;
        }

        public void setStsExpire(long stsExpire) {
            this.stsExpire = stsExpire;
        }
    }
}
