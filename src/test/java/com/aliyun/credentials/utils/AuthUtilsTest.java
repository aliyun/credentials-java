package com.aliyun.credentials.utils;

import com.aliyun.credentials.exception.CredentialException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

public class AuthUtilsTest {
    @Test
    public void getPrivateKeyTest() {
        new AuthUtils();
        AuthUtils.setPrivateKey(null);
        String path = AuthUtils.class.getClassLoader().getResource("configTest.ini").getPath();
        String privateKey = AuthUtils.getPrivateKey(path);
        Assert.assertNotNull(privateKey);
        Assert.assertEquals(privateKey, AuthUtils.getPrivateKey(path));
    }

    @Test
    public void environmentTest() {
        Assert.assertNull(AuthUtils.getEnvironmentAccessKeyId());
        AuthUtils.setEnvironmentAccessKeyId("test");
        Assert.assertEquals("test", AuthUtils.getEnvironmentAccessKeyId());
        AuthUtils.setEnvironmentAccessKeyId(null);

        Assert.assertNull(AuthUtils.getEnvironmentAccessKeySecret());
        AuthUtils.setEnvironmentAccessKeySecret("test");
        Assert.assertEquals("test", AuthUtils.getEnvironmentAccessKeySecret());
        AuthUtils.setEnvironmentAccessKeySecret(null);

        AuthUtils.setEnvironmentCredentialsFile("test");
        Assert.assertEquals("test", AuthUtils.getEnvironmentCredentialsFile());
        AuthUtils.setEnvironmentCredentialsFile(null);
        Assert.assertNull(AuthUtils.getEnvironmentCredentialsFile());

        Assert.assertNull(AuthUtils.getEnvironmentECSMetaData());
        AuthUtils.setEnvironmentECSMetaData("test");
        Assert.assertEquals("test", AuthUtils.getEnvironmentECSMetaData());
        AuthUtils.setEnvironmentECSMetaData(null);
        Assert.assertFalse(AuthUtils.environmentEnableOIDC());

        Assert.assertFalse(AuthUtils.getEnableECSIMDSv2());
        AuthUtils.enableECSIMDSv2(true);
        Assert.assertTrue(AuthUtils.getEnableECSIMDSv2());
        AuthUtils.enableECSIMDSv2(false);
        Assert.assertFalse(AuthUtils.getEnableECSIMDSv2());

        Assert.assertFalse(AuthUtils.getDisableECSIMDSv1());
        AuthUtils.disableECSIMDSv1(true);
        Assert.assertTrue(AuthUtils.getDisableECSIMDSv1());
        AuthUtils.disableECSIMDSv1(false);
        Assert.assertFalse(AuthUtils.getDisableECSIMDSv1());

        Assert.assertNull(AuthUtils.getEnvironmentRoleArn());
        AuthUtils.setEnvironmentRoleArn("test");
        Assert.assertEquals("test", AuthUtils.getEnvironmentRoleArn());
        Assert.assertFalse(AuthUtils.environmentEnableOIDC());

        Assert.assertNull(AuthUtils.getEnvironmentOIDCProviderArn());
        AuthUtils.setEnvironmentOIDCProviderArn("test");
        Assert.assertEquals("test", AuthUtils.getEnvironmentOIDCProviderArn());
        Assert.assertFalse(AuthUtils.environmentEnableOIDC());

        Assert.assertNull(AuthUtils.getEnvironmentOIDCTokenFilePath());
        AuthUtils.setEnvironmentOIDCTokenFilePath("test");
        Assert.assertEquals("test", AuthUtils.getEnvironmentOIDCTokenFilePath());
        Assert.assertTrue(AuthUtils.environmentEnableOIDC());

        AuthUtils.setEnvironmentRoleArn(null);
        AuthUtils.setEnvironmentOIDCProviderArn(null);
        AuthUtils.setEnvironmentOIDCTokenFilePath(null);
        Assert.assertFalse(AuthUtils.environmentEnableOIDC());

        Assert.assertNull(AuthUtils.getEnvironmentRoleSessionName());
        AuthUtils.setEnvironmentRoleSessionName("test");
        Assert.assertEquals("test", AuthUtils.getEnvironmentRoleSessionName());
        AuthUtils.setEnvironmentRoleSessionName(null);

        Assert.assertNull(AuthUtils.getEnvironmentSTSRegion());
        AuthUtils.setEnvironmentSTSRegion("test");
        Assert.assertEquals("test", AuthUtils.getEnvironmentSTSRegion());
        AuthUtils.setEnvironmentSTSRegion(null);

        Assert.assertFalse(AuthUtils.isDisableECSMetaData());
        AuthUtils.disableECSMetaData(true);
        Assert.assertTrue(AuthUtils.isDisableECSMetaData());
        AuthUtils.disableECSMetaData(false);
        Assert.assertFalse(AuthUtils.isDisableECSMetaData());

        Assert.assertFalse(AuthUtils.isEnableVpcEndpoint());
        AuthUtils.enableVpcEndpoint(true);
        Assert.assertTrue(AuthUtils.isEnableVpcEndpoint());
        AuthUtils.enableVpcEndpoint(false);
        Assert.assertFalse(AuthUtils.isEnableVpcEndpoint());

        Assert.assertFalse(AuthUtils.isDisableCLIProfile());
        AuthUtils.disableCLIProfile(true);
        Assert.assertTrue(AuthUtils.isDisableCLIProfile());
        AuthUtils.disableCLIProfile(false);
        Assert.assertFalse(AuthUtils.isDisableCLIProfile());
    }

    @Test
    public void environmentFlagsReadProcessEnvironmentWhenUnset() throws Exception {
        Assume.assumeTrue(EnvTestSupport.trySet("ALIBABA_CLOUD_ECS_IMDSV2_ENABLE", "true"));
        try {
            withStaticFlag("enableECSIMDSv2", () -> {
                Assert.assertTrue(AuthUtils.getEnableECSIMDSv2());
                Assert.assertTrue(EnvTestSupport.trySet("ALIBABA_CLOUD_ECS_IMDSV2_ENABLE", "false"));
                Assert.assertFalse(AuthUtils.getEnableECSIMDSv2());
            });
            Assert.assertTrue(EnvTestSupport.trySet("ALIBABA_CLOUD_IMDSV1_DISABLED", "true"));
            withStaticFlag("disableECSIMDSv1", () -> Assert.assertTrue(AuthUtils.getDisableECSIMDSv1()));
            Assert.assertTrue(EnvTestSupport.trySet("ALIBABA_CLOUD_CLI_PROFILE_DISABLED", "true"));
            withStaticFlag("disableCLIProfile", () -> Assert.assertTrue(AuthUtils.isDisableCLIProfile()));
            Assert.assertTrue(EnvTestSupport.trySet("ALIBABA_CLOUD_ECS_METADATA_DISABLED", "true"));
            withStaticFlag("disableECSMetaData", () -> Assert.assertTrue(AuthUtils.isDisableECSMetaData()));
            Assert.assertTrue(EnvTestSupport.trySet("ALIBABA_CLOUD_VPC_ENDPOINT_ENABLED", "true"));
            withStaticFlag("enableVpcEndpoint", () -> Assert.assertTrue(AuthUtils.isEnableVpcEndpoint()));
        } finally {
            EnvTestSupport.trySet("ALIBABA_CLOUD_ECS_IMDSV2_ENABLE", null);
            EnvTestSupport.trySet("ALIBABA_CLOUD_IMDSV1_DISABLED", null);
            EnvTestSupport.trySet("ALIBABA_CLOUD_CLI_PROFILE_DISABLED", null);
            EnvTestSupport.trySet("ALIBABA_CLOUD_ECS_METADATA_DISABLED", null);
            EnvTestSupport.trySet("ALIBABA_CLOUD_VPC_ENDPOINT_ENABLED", null);
        }
    }

    @Test
    public void getOIDCTokenRejectsUnreadableFile() throws Exception {
        Path file = Files.createTempFile("oidc-token", ".txt");
        Files.write(file, "token".getBytes("UTF-8"));
        Assume.assumeTrue(file.toFile().setReadable(false));
        try {
            if (file.toFile().canRead()) {
                return;
            }
            try {
                AuthUtils.getOIDCToken(file.toString());
                Assert.fail();
            } catch (CredentialException e) {
                Assert.assertTrue(e.getMessage().contains("cannot be read"));
            }
        } finally {
            file.toFile().setReadable(true);
        }
    }

    private static void withStaticFlag(String fieldName, Runnable action) throws Exception {
        Field field = AuthUtils.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object previous = field.get(null);
        field.set(null, null);
        try {
            action.run();
        } finally {
            field.set(null, previous);
        }
    }

    @Test
    public void clientTypeTest() {
        AuthUtils.setClientType(null);
        Assert.assertEquals("default", AuthUtils.getClientType());
        AuthUtils.setClientType("test");
        Assert.assertEquals("test", AuthUtils.getClientType());
    }
}
