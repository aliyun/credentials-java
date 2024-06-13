package com.aliyun.credentials.models;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ConfigTest {
    @Test
    public void buildTest() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "test");
        map.put("accessKeyId", "test");
        map.put("accessKeySecret", "test");
        map.put("roleArn", "test");
        map.put("roleSessionName", "test");
        map.put("privateKeyFile", "test");
        map.put("publicKeyId", "test");
        map.put("roleName", "test");
        map.put("enableIMDSv2", true);
        map.put("metadataTokenDuration", 180);
        map.put("bearerToken", "test");
        map.put("securityToken", "test");
        map.put("host", "test");
        map.put("readTimeout", 2000);
        map.put("connectTimeout", 2000);
        map.put("policy", "test");
        map.put("roleSessionExpiration", 1000);
        map.put("oidcProviderArn", "test");
        map.put("oidcTokenFilePath", "test");
        map.put("credentialsURI", "test");
        map.put("STSEndpoint", "test");
        map.put("externalId", "test");
        Config config = Config.build(map);
        Assert.assertEquals("test", config.getType());
        Assert.assertEquals("test", config.getAccessKeyId());
        Assert.assertEquals("test", config.getAccessKeySecret());
        Assert.assertEquals("test", config.getRoleArn());
        Assert.assertEquals("test", config.getRoleSessionName());
        Assert.assertEquals("test", config.getPrivateKeyFile());
        Assert.assertEquals("test", config.getPublicKeyId());
        Assert.assertEquals("test", config.getRoleName());
        Assert.assertEquals(true, config.getEnableIMDSv2());
        Assert.assertEquals(180, (int)config.getMetadataTokenDuration());
        Assert.assertEquals("test", config.getSecurityToken());
        Assert.assertEquals("test", config.getHost());
        Assert.assertEquals(2000, config.getTimeout());
        Assert.assertEquals(2000, config.getConnectTimeout());
        Assert.assertEquals("test", config.getPolicy());
        Assert.assertEquals(1000, config.getRoleSessionExpiration());
        Assert.assertEquals("test", config.getOidcProviderArn());
        Assert.assertEquals("test", config.getOidcTokenFilePath());
        Assert.assertEquals("test", config.getCredentialsUri());
        Assert.assertEquals("test", config.getSTSEndpoint());
        Assert.assertEquals("test", config.getExternalId());
    }
}
