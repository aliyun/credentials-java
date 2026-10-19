package com.aliyun.credentials.provider;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.utils.EnvTestSupport;
import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class CLIProfileCredentialsProviderTest {

    @Test
    public void getProfileNameTest() {
        CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
        Assert.assertNull(provider.getProfileName());
        provider = CLIProfileCredentialsProvider.builder()
                .profileName("AK")
                .build();
        Assert.assertEquals("AK", provider.getProfileName());
    }

    @Test
    public void shouldReloadCredentialsProviderTest() {
        CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
        Assert.assertTrue(provider.shouldReloadCredentialsProvider(""));
    }

    @Test
    public void disableCLIProfileTest() {
        boolean isDisableCLIProfile = AuthUtils.isDisableCLIProfile();
        AuthUtils.disableCLIProfile(true);
        CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("CLI credentials file is disabled.", e.getMessage());
        }
        AuthUtils.disableCLIProfile(isDisableCLIProfile);
    }

    @Test
    public void parseProfileTest() {
        CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
        try {
            provider.parseProfile("./not_exist_config.json");
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("Unable to open credentials file"));
        }
        try {
            String configPath = CLIProfileCredentialsProviderTest.class.getClassLoader().
                    getResource("invalid_cli_config.json").getPath();
            provider.parseProfile(configPath);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("Failed to parse credential form CLI credentials file"));
        }

        String configPath = CLIProfileCredentialsProviderTest.class.getClassLoader().
                getResource("empty_file.json").getPath();
        CLIProfileCredentialsProvider.Config config = provider.parseProfile(configPath);
        Assert.assertNull(config);

        configPath = CLIProfileCredentialsProviderTest.class.getClassLoader().
                getResource("mock_empty_cli_config.json").getPath();
        config = provider.parseProfile(configPath);
        Assert.assertNull(config.getCurrent());
        Assert.assertNull(config.getProfiles());

        configPath = CLIProfileCredentialsProviderTest.class.getClassLoader().
                getResource("full_cli_config.json").getPath();
        config = provider.parseProfile(configPath);
        Assert.assertEquals("AK", config.getCurrent());
        Assert.assertEquals(5, config.getProfiles().size());
        Assert.assertEquals("[{\"name\":\"AK\",\"mode\":\"AK\",\"access_key_id\":\"access_key_id\",\"access_key_secret\":\"access_key_secret\"},{\"name\":\"RamRoleArn\",\"mode\":\"RamRoleArn\",\"access_key_id\":\"access_key_id\",\"access_key_secret\":\"access_key_secret\",\"ram_role_arn\":\"ram_role_arn\",\"ram_session_name\":\"ram_session_name\",\"expired_seconds\":3600,\"sts_region\":\"cn-hangzhou\"},{\"name\":\"EcsRamRole\",\"mode\":\"EcsRamRole\",\"ram_role_name\":\"ram_role_name\"},{\"name\":\"OIDC\",\"mode\":\"OIDC\",\"ram_role_arn\":\"ram_role_arn\",\"ram_session_name\":\"ram_session_name\",\"expired_seconds\":3600,\"sts_region\":\"cn-hangzhou\",\"oidc_token_file\":\"path/to/oidc/file\",\"oidc_provider_arn\":\"oidc_provider_arn\"},{\"name\":\"ChainableRamRoleArn\",\"mode\":\"ChainableRamRoleArn\",\"ram_role_arn\":\"ram_role_arn\",\"ram_session_name\":\"ram_session_name\",\"expired_seconds\":3600,\"sts_region\":\"cn-hangzhou\",\"source_profile\":\"AK\"}]", new Gson().toJson(config.getProfiles()));
    }

    @Test
    public void reloadCredentialsProviderTest() {
        CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
        String configPath = CLIProfileCredentialsProviderTest.class.getClassLoader().
                getResource(".aliyun/config.json").getPath();
        CLIProfileCredentialsProvider.Config config = provider.parseProfile(configPath);
        try {
            provider.reloadCredentialsProvider(config, "inexist");
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Unable to get profile with 'inexist' form CLI credentials file.", e.getMessage());
        }

        AlibabaCloudCredentialsProvider credentialsProvider = provider.reloadCredentialsProvider(config, "AK");
        Assert.assertTrue(credentialsProvider instanceof StaticCredentialsProvider);
        CredentialModel credential = credentialsProvider.getCredentials();
        Assert.assertEquals("cli_profile", provider.getProviderName());
        Assert.assertEquals("static_ak", credentialsProvider.getProviderName());
        Assert.assertEquals("akid", credential.getAccessKeyId());
        Assert.assertEquals("secret", credential.getAccessKeySecret());
        Assert.assertNull(credential.getSecurityToken());

        credentialsProvider = provider.reloadCredentialsProvider(config, "StsToken");
        Assert.assertTrue(credentialsProvider instanceof StaticCredentialsProvider);
        credential = credentialsProvider.getCredentials();
        Assert.assertEquals("cli_profile", provider.getProviderName());
        Assert.assertEquals("static_sts", credentialsProvider.getProviderName());
        Assert.assertEquals("accessKeyId", credential.getAccessKeyId());
        Assert.assertEquals("accessKeySecret", credential.getAccessKeySecret());
        Assert.assertEquals("stsToken", credential.getSecurityToken());

        credentialsProvider = provider.reloadCredentialsProvider(config, "RamRoleArn");
        Assert.assertTrue(credentialsProvider instanceof RamRoleArnCredentialProvider);
        try {
            credentialsProvider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("InvalidAccessKeyId.NotFound"));
        }

        try {
            provider.reloadCredentialsProvider(config, "Invalid_RamRoleArn");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("AccessKeyId must not be null.", e.getMessage());
        }

        credentialsProvider = provider.reloadCredentialsProvider(config, "EcsRamRole");
        Assert.assertTrue(credentialsProvider instanceof EcsRamRoleCredentialProvider);

        credentialsProvider = provider.reloadCredentialsProvider(config, "OIDC");
        Assert.assertTrue(credentialsProvider instanceof OIDCRoleArnCredentialProvider);

        credentialsProvider = provider.reloadCredentialsProvider(config, "ChainableRamRoleArn");
        Assert.assertTrue(credentialsProvider instanceof RamRoleArnCredentialProvider);
        Assert.assertEquals("akid", ((RamRoleArnCredentialProvider) credentialsProvider).getAccessKeyId());
        Assert.assertEquals("secret", ((RamRoleArnCredentialProvider) credentialsProvider).getAccessKeySecret());

        try {
            provider.reloadCredentialsProvider(config, "ChainableRamRoleArn2");
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Unable to get profile with 'InvalidSource' form CLI credentials file.", e.getMessage());
        }

        try {
            provider.reloadCredentialsProvider(config, "Unsupported");
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Unsupported profile mode 'Unsupported' form CLI credentials file.", e.getMessage());
        }
    }

    @Test
    public void testCloudSSOMode() {
        CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
        String configPath = CLIProfileCredentialsProviderTest.class.getClassLoader().
                getResource(".aliyun/config.json").getPath();
        CLIProfileCredentialsProvider.Config config = provider.parseProfile(configPath);

        AlibabaCloudCredentialsProvider credentialsProvider = provider.reloadCredentialsProvider(config, "CloudSSO");
        Assert.assertTrue(credentialsProvider instanceof CloudSSOCredentialsProvider);
        Assert.assertEquals("cloud_sso", credentialsProvider.getProviderName());
    }

    @Test
    public void testOAuthModeCN() {
        CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
        String configPath = CLIProfileCredentialsProviderTest.class.getClassLoader().
                getResource(".aliyun/config.json").getPath();
        CLIProfileCredentialsProvider.Config config = provider.parseProfile(configPath);

        AlibabaCloudCredentialsProvider credentialsProvider = provider.reloadCredentialsProvider(config, "OAuthCN");
        Assert.assertTrue(credentialsProvider instanceof OAuthCredentialsProvider);
        Assert.assertEquals("oauth", credentialsProvider.getProviderName());
    }

    @Test
    public void testOAuthModeINTL() {
        CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
        String configPath = CLIProfileCredentialsProviderTest.class.getClassLoader().
                getResource(".aliyun/config.json").getPath();
        CLIProfileCredentialsProvider.Config config = provider.parseProfile(configPath);

        AlibabaCloudCredentialsProvider credentialsProvider = provider.reloadCredentialsProvider(config, "OAuthINTL");
        Assert.assertTrue(credentialsProvider instanceof OAuthCredentialsProvider);
        Assert.assertEquals("oauth", credentialsProvider.getProviderName());
    }

    @Test
    public void testOAuthModeInvalidSiteType() {
        CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
        String configPath = CLIProfileCredentialsProviderTest.class.getClassLoader().
                getResource(".aliyun/config.json").getPath();
        CLIProfileCredentialsProvider.Config config = provider.parseProfile(configPath);

        try {
            provider.reloadCredentialsProvider(config, "OAuthInvalid");
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Invalid OAuth site type, support CN or INTL.", e.getMessage());
        }
    }

    @Test
    public void testExternalMode() {
        Assume.assumeFalse(
                "external profile uses /bin/echo",
                System.getProperty("os.name").toLowerCase().contains("win")
        );
        CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
        String configPath = CLIProfileCredentialsProviderTest.class.getClassLoader().
                getResource(".aliyun/config.json").getPath();
        CLIProfileCredentialsProvider.Config config = provider.parseProfile(configPath);

        AlibabaCloudCredentialsProvider credentialsProvider = provider.reloadCredentialsProvider(config, "External");
        Assert.assertTrue(credentialsProvider instanceof ExternalCredentialsProvider);
        Assert.assertEquals("external", credentialsProvider.getProviderName());
        CredentialModel credential = credentialsProvider.getCredentials();
        Assert.assertEquals("externalAk", credential.getAccessKeyId());
        Assert.assertEquals("externalSk", credential.getAccessKeySecret());
    }

    @Test
    public void getCredentialsTest() {
        String homePath = System.getProperty("user.home");
        String configPath = CLIProfileCredentialsProviderTest.class.getClassLoader().
                getResource(".aliyun/config.json").getPath();
        String homeFromConfig = configPath.replace("/.aliyun/config.json", "");
        if (homeFromConfig.equals(configPath)) {
            homeFromConfig = configPath.replace("\\.aliyun\\config.json", "");
        }
        System.setProperty("user.home", homeFromConfig);
        CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
        CredentialModel credential = provider.getCredentials();
        Assert.assertEquals("akid", credential.getAccessKeyId());
        Assert.assertEquals("secret", credential.getAccessKeySecret());
        Assert.assertNull(credential.getSecurityToken());

        provider = CLIProfileCredentialsProvider.builder().profileName("inexist").build();
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Unable to get profile with 'inexist' form CLI credentials file.", e.getMessage());
        }

        provider = CLIProfileCredentialsProvider.builder().profileName("RamRoleArn").build();
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("InvalidAccessKeyId.NotFound"));
        }
        provider.close();

        System.setProperty("user.home", homePath);
    }

    @Test
    public void defaultConfigPathUsesFileSeparator() {
        String expected = new File(new File(System.getProperty("user.home"), ".aliyun"), "config.json").getPath();
        Assert.assertTrue(expected.endsWith(File.separator + ".aliyun" + File.separator + "config.json")
                || expected.endsWith("/.aliyun/config.json"));
        if (File.separatorChar == '\\') {
            Assert.assertFalse(expected.contains(".aliyun/config.json"));
        }
    }

    @Test
    public void getCredentialsRejectsEmptyCliFile() throws Exception {
        Path home = Files.createTempDirectory("cli-empty");
        String originalHome = System.getProperty("user.home");
        try {
            Path config = cliConfig(home);
            Files.createDirectories(config.getParent());
            Files.write(config, new byte[0]);
            System.setProperty("user.home", home.toString());
            CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
            try {
                provider.getCredentials();
                Assert.fail();
            } catch (CredentialException e) {
                Assert.assertEquals("Unable to get profile from empty CLI credentials file.", e.getMessage());
            }
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    public void shouldReloadAfterProviderIsCached() throws Exception {
        withCliHome(akAndStsConfig(), home -> {
            CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder()
                    .profileName("AK")
                    .build();
            Assert.assertTrue(provider.shouldReloadCredentialsProvider("AK"));
            Assert.assertEquals("akid", provider.getCredentials().getAccessKeyId());
            Assert.assertFalse(provider.shouldReloadCredentialsProvider("AK"));
            Assert.assertFalse(provider.shouldReloadCredentialsProvider(""));
            Assert.assertFalse(provider.shouldReloadCredentialsProvider(null));
            Assert.assertTrue(provider.shouldReloadCredentialsProvider("StsToken"));

            CLIProfileCredentialsProvider unnamed = CLIProfileCredentialsProvider.builder().build();
            Assert.assertEquals("akid", unnamed.getCredentials().getAccessKeyId());
            Assert.assertFalse(unnamed.shouldReloadCredentialsProvider("other"));
        });
    }

    @Test
    public void reloadsWhenEnvironmentProfileChanges() throws Exception {
        String previous = System.getenv("ALIBABA_CLOUD_PROFILE");
        Assume.assumeTrue("cannot set ALIBABA_CLOUD_PROFILE",
                EnvTestSupport.trySet("ALIBABA_CLOUD_PROFILE", "AK"));
        try {
            withCliHome(akAndStsConfig(), home -> {
                CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
                Assert.assertEquals("AK", provider.getProfileName());
                Assert.assertEquals("akid", provider.getCredentials().getAccessKeyId());
                Assert.assertTrue(EnvTestSupport.trySet("ALIBABA_CLOUD_PROFILE", "StsToken"));
                CredentialModel credential = provider.getCredentials();
                Assert.assertEquals("accessKeyId", credential.getAccessKeyId());
                Assert.assertEquals("stsToken", credential.getSecurityToken());
                Assert.assertEquals("StsToken", provider.getProfileName());
                Assert.assertTrue(credential.getProviderName().contains("cli_profile"));
            });
        } finally {
            EnvTestSupport.trySet("ALIBABA_CLOUD_PROFILE", previous);
        }
    }

    @Test
    public void oauthCallbackPersistsTokensAndFollowsSourceProfile() throws Exception {
        withCliHome(oauthConfig(), home -> {
            CLIProfileCredentialsProvider plain = CLIProfileCredentialsProvider.builder()
                    .profileName("plain")
                    .build();
            CLIProfileCredentialsProvider.Config config = plain.parseProfile(cliConfig(home).toString());
            invokeOAuthUpdate(plain, config, "oauth");
            String unchanged = new String(Files.readAllBytes(cliConfig(home)), StandardCharsets.UTF_8);
            Assert.assertFalse(unchanged.contains("new-refresh"));

            CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder()
                    .profileName("chain")
                    .build();
            invokeOAuthUpdate(provider, config, "oauth");
            String updated = new String(Files.readAllBytes(cliConfig(home)), StandardCharsets.UTF_8);
            Assert.assertTrue(updated.contains("new-refresh"));
            Assert.assertTrue(updated.contains("new-access"));
            Assert.assertTrue(updated.contains("new-ak"));
            Assert.assertTrue(updated.contains("new-sk"));
            Assert.assertTrue(updated.contains("new-token"));
        });
    }

    @Test
    public void oauthCallbackUsesCurrentProfileWhenNameIsEmpty() throws Exception {
        String previous = System.getenv("ALIBABA_CLOUD_PROFILE");
        Assume.assumeTrue(EnvTestSupport.trySet("ALIBABA_CLOUD_PROFILE", null));
        try {
            withCliHome(oauthConfig(), home -> {
                CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
                Assert.assertNull(provider.getProfileName());
                CLIProfileCredentialsProvider.Config config = provider.parseProfile(cliConfig(home).toString());
                invokeOAuthUpdate(provider, config, "oauth");
                String updated = new String(Files.readAllBytes(cliConfig(home)), StandardCharsets.UTF_8);
                Assert.assertTrue(updated.contains("new-refresh"));
            });
        } finally {
            EnvTestSupport.trySet("ALIBABA_CLOUD_PROFILE", previous);
        }
    }

    @Test
    public void oauthCallbackReturnsWhenFileIsMissingNullOrProfileIsUnknown() throws Exception {
        withCliHome(oauthConfig(), home -> {
            Path configPath = cliConfig(home);
            CLIProfileCredentialsProvider missingFile = CLIProfileCredentialsProvider.builder()
                    .profileName("oauth")
                    .build();
            CLIProfileCredentialsProvider.Config config = missingFile.parseProfile(configPath.toString());
            Files.delete(configPath);
            invokeOAuthUpdate(missingFile, config, "oauth");
            Assert.assertFalse(Files.exists(configPath));

            Files.write(configPath, "null".getBytes(StandardCharsets.UTF_8));
            invokeOAuthUpdate(missingFile, config, "oauth");
            Assert.assertEquals("null", new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8));

            Files.write(configPath, "{\"current\":\"oauth\"}".getBytes(StandardCharsets.UTF_8));
            invokeOAuthUpdate(missingFile, config, "oauth");
            Assert.assertFalse(new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8).contains("new-ak"));

            Files.write(configPath, oauthConfig().getBytes(StandardCharsets.UTF_8));
            CLIProfileCredentialsProvider unknown = CLIProfileCredentialsProvider.builder()
                    .profileName("missing")
                    .build();
            invokeOAuthUpdate(unknown, unknown.parseProfile(configPath.toString()), "oauth");
            Assert.assertFalse(new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8).contains("new-ak"));
        });
    }

    @Test
    public void oauthCallbackSwallowsIoFailure() throws Exception {
        withCliHome(oauthConfig(), home -> {
            Path configPath = cliConfig(home);
            CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder()
                    .profileName("oauth")
                    .build();
            CLIProfileCredentialsProvider.Config config = provider.parseProfile(configPath.toString());
            Files.delete(configPath);
            Files.createDirectory(configPath);
            invokeOAuthUpdate(provider, config, "oauth");
            Assert.assertTrue(Files.isDirectory(configPath));
        });
    }

    @Test
    public void externalCallbackPersistsCredentialsAndFollowsSourceProfile() throws Exception {
        withCliHome(externalConfig(), home -> {
            CLIProfileCredentialsProvider plain = CLIProfileCredentialsProvider.builder()
                    .profileName("plain")
                    .build();
            CLIProfileCredentialsProvider.Config config = plain.parseProfile(cliConfig(home).toString());
            invokeExternalUpdate(plain, config, "external");
            Assert.assertFalse(new String(Files.readAllBytes(cliConfig(home)), StandardCharsets.UTF_8).contains("ext-ak"));

            CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder()
                    .profileName("chain")
                    .build();
            invokeExternalUpdate(provider, config, "external");
            String updated = new String(Files.readAllBytes(cliConfig(home)), StandardCharsets.UTF_8);
            Assert.assertTrue(updated.contains("ext-ak"));
            Assert.assertTrue(updated.contains("ext-sk"));
            Assert.assertTrue(updated.contains("ext-token"));
        });
    }

    @Test
    public void externalCallbackReturnsOnMissingFileNullConfigAndUnknownProfile() throws Exception {
        withCliHome(externalConfig(), home -> {
            Path configPath = cliConfig(home);
            CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder()
                    .profileName("external")
                    .build();
            CLIProfileCredentialsProvider.Config config = provider.parseProfile(configPath.toString());
            Files.delete(configPath);
            invokeExternalUpdate(provider, config, "external");
            Assert.assertFalse(Files.exists(configPath));

            Files.write(configPath, "null".getBytes(StandardCharsets.UTF_8));
            invokeExternalUpdate(provider, config, "external");
            Assert.assertEquals("null", new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8));

            Files.write(configPath, "{\"current\":\"external\"}".getBytes(StandardCharsets.UTF_8));
            invokeExternalUpdate(provider, config, "external");
            Assert.assertFalse(new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8).contains("ext-ak"));

            Files.write(configPath, externalConfig().getBytes(StandardCharsets.UTF_8));
            CLIProfileCredentialsProvider unknown = CLIProfileCredentialsProvider.builder()
                    .profileName("missing")
                    .build();
            invokeExternalUpdate(unknown, unknown.parseProfile(configPath.toString()), "external");
            Assert.assertFalse(new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8).contains("ext-ak"));

            Files.delete(configPath);
            Files.createDirectory(configPath);
            invokeExternalUpdate(provider, config, "external");
            Assert.assertTrue(Files.isDirectory(configPath));
        });
    }

    @Test
    public void reloadRejectsEmptyProfileListsAndNullOAuthSite() throws Exception {
        CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
        Path home = Files.createTempDirectory("cli-edges");
        Path configPath = cliConfig(home);
        Files.createDirectories(configPath.getParent());
        Files.write(configPath, "{\"current\":\"AK\",\"profiles\":[]}".getBytes(StandardCharsets.UTF_8));
        try {
            provider.reloadCredentialsProvider(provider.parseProfile(configPath.toString()), "AK");
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Unable to get profile with 'AK' form CLI credentials file.", e.getMessage());
        }
        Files.write(configPath, "{\"current\":\"AK\"}".getBytes(StandardCharsets.UTF_8));
        try {
            provider.reloadCredentialsProvider(provider.parseProfile(configPath.toString()), null);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Unable to get profile with 'AK' form CLI credentials file.", e.getMessage());
        }

        String edgeConfig = "{"
                + "\"current\":\"oauth-null-site\","
                + "\"profiles\":["
                + "{\"name\":\"\",\"mode\":\"AK\"},"
                + "{\"name\":\"oauth-null-site\",\"mode\":\"OAuth\",\"oauth_refresh_token\":\"r\",\"oauth_access_token\":\"a\"},"
                + "{\"name\":\"oauth-lower\",\"mode\":\"OAuth\",\"oauth_site_type\":\"cn\",\"oauth_refresh_token\":\"r\",\"oauth_access_token\":\"a\"},"
                + "{\"name\":\"sso-no-expire\",\"mode\":\"CloudSSO\",\"cloud_sso_sign_in_url\":\"https://signin.example.com\",\"cloud_sso_account_id\":\"1\",\"cloud_sso_access_config\":\"ac\",\"access_token\":\"tok\"},"
                + "{\"name\":\"not-oauth\",\"mode\":\"AK\",\"access_key_id\":\"akid\",\"access_key_secret\":\"secret\"}"
                + "]}";
        Files.write(configPath, edgeConfig.getBytes(StandardCharsets.UTF_8));
        CLIProfileCredentialsProvider.Config config = provider.parseProfile(configPath.toString());
        try {
            provider.reloadCredentialsProvider(config, "oauth-null-site");
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Invalid OAuth site type, support CN or INTL.", e.getMessage());
        }
        Assert.assertTrue(provider.reloadCredentialsProvider(config, "oauth-lower") instanceof OAuthCredentialsProvider);
        try {
            provider.reloadCredentialsProvider(config, "sso-no-expire");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("CloudSSO access token is empty or expired, please re-login with cli.", e.getMessage());
        }
    }

    @Test
    public void parseProfileRejectsUnreadablePath() throws Exception {
        CLIProfileCredentialsProvider provider = CLIProfileCredentialsProvider.builder().build();
        Path dir = Files.createTempDirectory("cli-not-file");
        try {
            provider.parseProfile(dir.toString());
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("Unable to open credentials file"));
        }

        Path file = dir.resolve("config.json");
        Files.write(file, "{}".getBytes(StandardCharsets.UTF_8));
        Assume.assumeTrue(file.toFile().setReadable(false));
        try {
            if (file.toFile().canRead()) {
                return;
            }
            try {
                provider.parseProfile(file.toString());
                Assert.fail();
            } catch (CredentialException e) {
                Assert.assertTrue(e.getMessage().contains("Unable to open credentials file"));
            }
        } finally {
            file.toFile().setReadable(true);
        }
    }

    private static void withCliHome(String json, CliHomeAssertion assertion) throws Exception {
        Path home = Files.createTempDirectory("cli-home");
        String originalHome = System.getProperty("user.home");
        try {
            Path config = cliConfig(home);
            Files.createDirectories(config.getParent());
            Files.write(config, json.getBytes(StandardCharsets.UTF_8));
            System.setProperty("user.home", home.toString());
            assertion.check(home);
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    private static Path cliConfig(Path home) {
        return home.resolve(".aliyun").resolve("config.json");
    }

    private static void invokeOAuthUpdate(CLIProfileCredentialsProvider provider,
                                           CLIProfileCredentialsProvider.Config config,
                                           String profileName) throws Exception {
        OAuthCredentialsProvider inner = (OAuthCredentialsProvider) provider.reloadCredentialsProvider(config, profileName);
        Field field = OAuthCredentialsProvider.class.getDeclaredField("tokenUpdateCallback");
        field.setAccessible(true);
        OAuthCredentialsProvider.OAuthTokenUpdateCallback callback =
                (OAuthCredentialsProvider.OAuthTokenUpdateCallback) field.get(inner);
        callback.onTokenUpdate("new-refresh", "new-access", "new-ak", "new-sk", "new-token", 123456L, 654321L);
    }

    private static void invokeExternalUpdate(CLIProfileCredentialsProvider provider,
                                              CLIProfileCredentialsProvider.Config config,
                                              String profileName) throws Exception {
        ExternalCredentialsProvider inner =
                (ExternalCredentialsProvider) provider.reloadCredentialsProvider(config, profileName);
        Field field = ExternalCredentialsProvider.class.getDeclaredField("credentialUpdateCallback");
        field.setAccessible(true);
        ExternalCredentialsProvider.ExternalCredentialUpdateCallback callback =
                (ExternalCredentialsProvider.ExternalCredentialUpdateCallback) field.get(inner);
        callback.onCredentialUpdate("ext-ak", "ext-sk", "ext-token", 1710000000L);
    }

    private static String akAndStsConfig() {
        return "{"
                + "\"current\":\"AK\","
                + "\"profiles\":["
                + "{\"name\":\"AK\",\"mode\":\"AK\",\"access_key_id\":\"akid\",\"access_key_secret\":\"secret\"},"
                + "{\"name\":\"StsToken\",\"mode\":\"StsToken\",\"access_key_id\":\"accessKeyId\",\"access_key_secret\":\"accessKeySecret\",\"sts_token\":\"stsToken\"}"
                + "]}";
    }

    private static String oauthConfig() {
        return "{"
                + "\"current\":\"oauth\","
                + "\"profiles\":["
                + "{\"name\":\"\",\"mode\":\"AK\"},"
                + "{\"name\":\"chain\",\"mode\":\"ChainableRamRoleArn\",\"source_profile\":\"oauth\",\"ram_role_arn\":\"arn\"},"
                + "{\"name\":\"oauth\",\"mode\":\"OAuth\",\"oauth_site_type\":\"CN\",\"oauth_refresh_token\":\"old-refresh\",\"oauth_access_token\":\"old-access\",\"oauth_access_token_expire\":10},"
                + "{\"name\":\"plain\",\"mode\":\"AK\",\"access_key_id\":\"akid\",\"access_key_secret\":\"secret\"}"
                + "]}";
    }

    private static String externalConfig() {
        return "{"
                + "\"current\":\"external\","
                + "\"profiles\":["
                + "{\"name\":\"chain\",\"mode\":\"RamRoleArn\",\"source_profile\":\"external\",\"access_key_id\":\"akid\",\"access_key_secret\":\"secret\",\"ram_role_arn\":\"arn\"},"
                + "{\"name\":\"external\",\"mode\":\"External\",\"process_command\":\"/bin/echo ok\"},"
                + "{\"name\":\"plain\",\"mode\":\"AK\",\"access_key_id\":\"akid\",\"access_key_secret\":\"secret\"}"
                + "]}";
    }

    private interface CliHomeAssertion {
        void check(Path home) throws Exception;
    }
}
