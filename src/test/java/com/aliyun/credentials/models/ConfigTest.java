package com.aliyun.credentials.models;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ConfigTest {

    @Test
    public void setterTest() {
        Config config = new Config()
                .setType("test")
                .setAccessKeyId("test")
                .setAccessKeySecret("test")
                .setRoleArn("test")
                .setRoleSessionName("test")
                .setPrivateKeyFile("test")
                .setPublicKeyId("test")
                .setRoleName("test")
                .setDisableIMDSv1(true)
                .setBearerToken("test")
                .setSecurityToken("test")
                .setHost("test")
                .setTimeout(2000)
                .setConnectTimeout(2000)
                .setPolicy("test")
                .setRoleSessionExpiration(1000)
                .setOidcProviderArn("test")
                .setOidcTokenFilePath("test")
                .setCredentialsUri("test")
                .setStsEndpoint("test")
                .setExternalId("test")
                .setProxy("test");
        Assert.assertEquals("test", config.getType());
        Assert.assertEquals("test", config.getAccessKeyId());
        Assert.assertEquals("test", config.getAccessKeySecret());
        Assert.assertEquals("test", config.getRoleArn());
        Assert.assertEquals("test", config.getRoleSessionName());
        Assert.assertEquals("test", config.getPrivateKeyFile());
        Assert.assertEquals("test", config.getPublicKeyId());
        Assert.assertEquals("test", config.getRoleName());
        Assert.assertEquals(true, config.getDisableIMDSv1());
        Assert.assertEquals("test", config.getSecurityToken());
        Assert.assertEquals("test", config.getHost());
        Assert.assertEquals(2000, (int) config.getTimeout());
        Assert.assertEquals(2000, (int) config.getConnectTimeout());
        Assert.assertEquals("test", config.getPolicy());
        Assert.assertEquals(1000, config.getRoleSessionExpiration());
        Assert.assertEquals("test", config.getOidcProviderArn());
        Assert.assertEquals("test", config.getOidcTokenFilePath());
        Assert.assertEquals("test", config.getCredentialsUri());
        Assert.assertEquals("test", config.getExternalId());
        Assert.assertEquals("test", config.getProxy());
    }
}
