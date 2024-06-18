package com.aliyun.credentials.provider;

import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.models.Config;
import org.junit.Assert;
import org.junit.Test;

public class RsaKeyPairCredentialProviderTest {
    @Test
    public void constructorTest() {
        Configuration config = new Configuration();
        config.setPublicKeyId("test");
        config.setPrivateKeyFile("test");
        config.setConnectTimeout(2000);
        config.setReadTimeout(2000);
        RsaKeyPairCredentialProvider provider = new RsaKeyPairCredentialProvider(config);
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals("test", provider.getPrivateKey());
        Assert.assertEquals("test", provider.getPublicKeyId());
        config.setSTSEndpoint("sts.cn-hangzhou.aliyuncs.com");
        provider = new RsaKeyPairCredentialProvider(config);
        Assert.assertEquals("sts.cn-hangzhou.aliyuncs.com", provider.getSTSEndpoint());

        Config config1 = new Config();
        config1.publicKeyId = "test";
        config1.privateKeyFile = "test";
        config1.connectTimeout = 2000;
        config1.timeout = 2000;
        provider = new RsaKeyPairCredentialProvider(config1);
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals("test", provider.getPrivateKey());
        Assert.assertEquals("test", provider.getPublicKeyId());
        config1.STSEndpoint = "sts.cn-hangzhou.aliyuncs.com";
        provider = new RsaKeyPairCredentialProvider(config);
        Assert.assertEquals("sts.cn-hangzhou.aliyuncs.com", provider.getSTSEndpoint());
    }

    @Test
    public void getCredentialsTest() {
        RsaKeyPairCredentialProvider provider = new RsaKeyPairCredentialProvider(null, null);
        Assert.assertNull(provider.getCredentials());
    }

    @Test
    public void getSet() {
        RsaKeyPairCredentialProvider provider = new RsaKeyPairCredentialProvider("test", "test");
        provider.setConnectTimeout(888);
        Assert.assertEquals(888, provider.getConnectTimeout());

        provider.setReadTimeout(999);
        Assert.assertEquals(999, provider.getReadTimeout());

        provider.setRegionId("test");
        Assert.assertEquals("test", provider.getRegionId());

        provider.setDurationSeconds(2000);
        Assert.assertEquals(2000, provider.getDurationSeconds());

        provider.setPrivateKey("test");
        Assert.assertEquals("test", provider.getPrivateKey());

        provider.setPublicKeyId("test");
        Assert.assertEquals("test", provider.getPublicKeyId());

        provider.setSTSEndpoint("www.aliyun.com");
        Assert.assertEquals("www.aliyun.com", provider.getSTSEndpoint());
    }
}
