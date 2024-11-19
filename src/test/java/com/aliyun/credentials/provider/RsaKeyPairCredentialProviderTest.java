package com.aliyun.credentials.provider;

import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.FormatType;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RsaKeyPairCredentialProviderTest {
    @Test
    public void constructorTest() {
        try {
            new RsaKeyPairCredentialProvider(null, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("PrivateKeyFile must not be null.", e.getMessage());
        }
        Configuration config = new Configuration();
        String file = ProfileCredentialsProviderTest.class.getClassLoader().
                getResource("private_key.txt").getPath();
        config.setPublicKeyId("test");
        config.setPrivateKeyFile(file);
        config.setConnectTimeout(2000);
        config.setReadTimeout(2000);
        RsaKeyPairCredentialProvider provider = new RsaKeyPairCredentialProvider(config);
        Assert.assertEquals("rsa_key_pair", provider.getProviderName());
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals("test", provider.getPrivateKey());
        Assert.assertEquals("test", provider.getPublicKeyId());
        config.setSTSEndpoint("sts.cn-hangzhou.aliyuncs.com");
        provider = new RsaKeyPairCredentialProvider(config);
        Assert.assertEquals("sts.cn-hangzhou.aliyuncs.com", provider.getSTSEndpoint());

        Config config1 = new Config();
        config1.publicKeyId = "test";
        config1.privateKeyFile = file;
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
        String file = ProfileCredentialsProviderTest.class.getClassLoader().
                getResource("private_key.txt").getPath();
        RsaKeyPairCredentialProvider provider = new RsaKeyPairCredentialProvider("test", file);
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Error refreshing credentials from RsaKeyPair"));
        }
    }

    @Test
    public void createCredentialTest() {
        String file = ProfileCredentialsProviderTest.class.getClassLoader().
                getResource("private_key.txt").getPath();
        RsaKeyPairCredentialProvider provider = new RsaKeyPairCredentialProvider("test", file);
        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setResponseCode(200);
        response.setHttpContent(new String("{\"SessionAccessKey\":{\"Expiration\":\"2019-12-12T1:1:1Z\",\"SessionAccessKeyId\":\"test\"," +
                "\"SessionAccessKeySecret\":\"test\"}}").getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        Assert.assertEquals(AuthConstant.RSA_KEY_PAIR, provider.createCredential(client).value().getType());
    }

    @Test
    public void getSet() {
        String file = ProfileCredentialsProviderTest.class.getClassLoader().
                getResource("private_key.txt").getPath();
        RsaKeyPairCredentialProvider provider = new RsaKeyPairCredentialProvider("test", file);
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

    @Test
    public void builderTest() {
        RsaKeyPairCredentialProvider provider;
        try {
            RsaKeyPairCredentialProvider.builder().build();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("PublicKeyId must not be null.", e.getMessage());
        }

        try {
            RsaKeyPairCredentialProvider.builder()
                    .publicKeyId("test")
                    .build();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("PrivateKey must not be null.", e.getMessage());
        }

        try {
            RsaKeyPairCredentialProvider.builder()
                    .durationSeconds(100)
                    .build();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("Session duration should be in the range of 900s - max session duration.", e.getMessage());
        }

        String file = ProfileCredentialsProviderTest.class.getClassLoader().
                getResource("private_key.txt").getPath();

        provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKeyFile(file)
                .build();
        Assert.assertEquals("sts.ap-northeast-1.aliyuncs.com", provider.getSTSEndpoint());

        AuthUtils.setEnvironmentSTSRegion("cn-beijing");
        provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKey("test")
                .build();
        Assert.assertEquals("sts.cn-beijing.aliyuncs.com", provider.getSTSEndpoint());

        provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKeyFile(file)
                .stsRegionId("cn-hangzhou")
                .build();
        Assert.assertEquals("sts.cn-hangzhou.aliyuncs.com", provider.getSTSEndpoint());

        AuthUtils.enableVpcEndpoint(true);
        provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKeyFile(file)
                .stsRegionId("cn-hangzhou")
                .build();
        Assert.assertEquals("sts-vpc.cn-hangzhou.aliyuncs.com", provider.getSTSEndpoint());

        provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKeyFile(file)
                .stsRegionId("cn-hangzhou")
                .enableVpc(true)
                .build();
        Assert.assertEquals("sts-vpc.cn-hangzhou.aliyuncs.com", provider.getSTSEndpoint());

        provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKeyFile(file)
                .STSEndpoint("sts.cn-shanghai.aliyuncs.com")
                .stsRegionId("cn-hangzhou")
                .enableVpc(true)
                .build();
        Assert.assertEquals("sts.cn-shanghai.aliyuncs.com", provider.getSTSEndpoint());

        provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKeyFile(file)
                .durationSeconds(1000)
                .STSEndpoint("sts.aliyuncs.com")
                .regionId("cn-hangzhou")
                .connectionTimeout(2000)
                .readTimeout(2000)
                .build();
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals(1000, provider.getDurationSeconds());
        Assert.assertEquals("test", provider.getPublicKeyId());
        Assert.assertEquals("sts.aliyuncs.com", provider.getSTSEndpoint());
        Assert.assertEquals("cn-hangzhou", provider.getRegionId());

        AuthUtils.setEnvironmentSTSRegion(null);
        AuthUtils.enableVpcEndpoint(false);
        provider.close();
    }
}
