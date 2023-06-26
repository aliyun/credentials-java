package models;

import com.aliyun.credentials.models.Config;
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
        Assert.assertEquals("test", config.getBearerToken());
        Assert.assertEquals("test", config.getSecurityToken());
        Assert.assertEquals("test", config.host);
        Assert.assertEquals(2000, config.timeout);
        Assert.assertEquals(2000, config.connectTimeout);
        Assert.assertEquals("test", config.getPolicy());
        Assert.assertEquals(1000, config.getRoleSessionExpiration());
        Assert.assertEquals("test", config.oidcProviderArn);
        Assert.assertEquals("test", config.oidcTokenFilePath);
        Assert.assertEquals("test", config.getCredentialsUri());
        Assert.assertEquals("test", config.getSTSEndpoint());
        Assert.assertEquals("test", config.getExternalId());
    }
}
