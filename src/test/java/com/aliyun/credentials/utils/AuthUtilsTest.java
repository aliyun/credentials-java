package com.aliyun.credentials.utils;

import org.junit.Assert;
import org.junit.Test;

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

        Assert.assertFalse(AuthUtils.getDisableECSIMDSv1());
        AuthUtils.disableECSIMDSv1(true);
        Assert.assertTrue(AuthUtils.getDisableECSIMDSv1());

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
    }

    @Test
    public void clientTypeTest() {
        AuthUtils.setClientType(null);
        Assert.assertEquals("default", AuthUtils.getClientType());
        AuthUtils.setClientType("test");
        Assert.assertEquals("test", AuthUtils.getClientType());
    }
}
