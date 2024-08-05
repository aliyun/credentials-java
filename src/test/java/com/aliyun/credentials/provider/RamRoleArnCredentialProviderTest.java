package com.aliyun.credentials.provider;

import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.models.Config;
import org.junit.Assert;
import org.junit.Test;

public class RamRoleArnCredentialProviderTest {

    @Test
    public void constructorTest() {
        RamRoleArnCredentialProvider provider = new RamRoleArnCredentialProvider("id", "secret",
                "name", "arn", "region", "policy");
        Assert.assertEquals("name", provider.getRoleSessionName());
        Assert.assertEquals("region", provider.getRegionId());
        Assert.assertEquals("policy", provider.getPolicy());
        Assert.assertEquals("id", provider.getAccessKeyId());
        Assert.assertEquals("secret", provider.getAccessKeySecret());
        Assert.assertEquals("arn", provider.getRoleArn());

        Configuration config = new Configuration();
        config.setAccessKeyId("test");
        config.setAccessKeySecret("test");
        config.setRoleArn("test");
        config.setOIDCProviderArn("test");
        config.setRoleSessionName("test");
        config.setConnectTimeout(2000);
        config.setReadTimeout(2000);
        provider = new RamRoleArnCredentialProvider(config);
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals("test", provider.getAccessKeyId());
        Assert.assertEquals("test", provider.getAccessKeySecret());
        Assert.assertEquals("test", provider.getRoleArn());
        Assert.assertEquals("test", provider.getRoleSessionName());
        Assert.assertNull(provider.getPolicy());
        config.setSTSEndpoint("sts.cn-hangzhou.aliyuncs.com");
        provider = new RamRoleArnCredentialProvider(config);
        Assert.assertEquals("sts.cn-hangzhou.aliyuncs.com", provider.getSTSEndpoint());

        Config config1 = new Config();
        config1.accessKeyId = "test";
        config1.accessKeySecret = "test";
        config1.roleArn = "test";
        config1.oidcProviderArn = "test";
        config1.roleSessionName = "test";
        config1.policy = "test";
        config1.roleSessionExpiration = 1000;
        config1.connectTimeout = 2000;
        config1.timeout = 2000;
        provider = new RamRoleArnCredentialProvider(config1);
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals(1000, provider.getDurationSeconds());
        Assert.assertEquals("test", provider.getAccessKeyId());
        Assert.assertEquals("test", provider.getAccessKeySecret());
        Assert.assertEquals("test", provider.getRoleArn());
        Assert.assertEquals("test", provider.getRoleSessionName());
        Assert.assertEquals("test", provider.getPolicy());
        config1.STSEndpoint = "sts.cn-hangzhou.aliyuncs.com";
        provider = new RamRoleArnCredentialProvider(config1);
        Assert.assertEquals("sts.cn-hangzhou.aliyuncs.com", provider.getSTSEndpoint());
    }

    @Test
    public void getCredentials() {
        Configuration config = new Configuration();
        config.setAccessKeyId("test");
        config.setAccessKeySecret("test");
        config.setRoleArn("test");
        config.setRoleSessionName("test");
        config.setConnectTimeout(2000);
        config.setReadTimeout(2000);
        RamRoleArnCredentialProvider provider = new RamRoleArnCredentialProvider(config);
        provider.setPolicy("test");
        provider.setExternalId("test");
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals("test", provider.getAccessKeyId());
        Assert.assertEquals("test", provider.getAccessKeySecret());
        Assert.assertEquals("test", provider.getRoleArn());
        Assert.assertEquals("test", provider.getRoleSessionName());
        Assert.assertEquals("test", provider.getExternalId());
        Assert.assertNull(provider.getCredentials());
    }

    @Test
    public void getSetTest() {
        RamRoleArnCredentialProvider provider = new RamRoleArnCredentialProvider(null, null, null);
        provider.setConnectTimeout(888);
        Assert.assertEquals(888, provider.getConnectTimeout());

        provider.setReadTimeout(999);
        Assert.assertEquals(999, provider.getReadTimeout());

        provider.setPolicy("test");
        Assert.assertEquals("test", provider.getPolicy());

        provider.setRegionId("test");
        Assert.assertEquals("test", provider.getRegionId());

        provider.setRoleSessionName("test");
        Assert.assertEquals("test", provider.getRoleSessionName());

        provider.setDurationSeconds(2000);
        Assert.assertEquals(2000, provider.getDurationSeconds());

        provider.setAccessKeyId("test");
        Assert.assertEquals("test", provider.getAccessKeyId());

        provider.setAccessKeySecret("test");
        Assert.assertEquals("test", provider.getAccessKeySecret());

        provider.setSTSEndpoint("www.aliyun.com");
        Assert.assertEquals("www.aliyun.com", provider.getSTSEndpoint());
    }

    @Test
    public void builderTest() {
        RamRoleArnCredentialProvider provider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .durationSeconds(1000)
                .roleArn("test")
                .roleSessionName("test")
                .policy("test")
                .STSEndpoint("sts.cn-hangzhou.aliyuncs.com")
                .regionId("cn-hangzhou")
                .externalId("test")
                .connectionTimeout(2000)
                .readTimeout(2000)
                .build();
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals(1000, provider.getDurationSeconds());
        Assert.assertEquals("test", provider.getAccessKeyId());
        Assert.assertEquals("test", provider.getAccessKeySecret());
        Assert.assertEquals("test", provider.getRoleArn());
        Assert.assertEquals("test", provider.getRoleSessionName());
        Assert.assertEquals("test", provider.getPolicy());
        Assert.assertEquals("sts.cn-hangzhou.aliyuncs.com", provider.getSTSEndpoint());
        Assert.assertEquals("cn-hangzhou", provider.getRegionId());
        Assert.assertEquals("test", provider.getExternalId());
        Assert.assertNull(provider.getCredentials());
    }

}