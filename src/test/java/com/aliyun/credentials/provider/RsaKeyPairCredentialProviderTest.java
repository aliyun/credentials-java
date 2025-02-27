package com.aliyun.credentials.provider;

import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.FormatType;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.configure.Config;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RsaKeyPairCredentialProviderTest {

    @Test
    public void getCredentialsTest() {
        String file = ProfileCredentialsProviderTest.class.getClassLoader().
                getResource("private_key.txt").getPath();
        RsaKeyPairCredentialProvider provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKeyFile(file)
                .build();
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
        RsaKeyPairCredentialProvider provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKeyFile(file)
                .build();
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
        RsaKeyPairCredentialProvider provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKeyFile(file)
                .build();
        provider.setConnectTimeout(888);
        Assert.assertEquals(888, provider.getConnectTimeout());

        provider.setReadTimeout(999);
        Assert.assertEquals(999, provider.getReadTimeout());

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
        Assert.assertEquals(Config.STS_DEFAULT_ENDPOINT, provider.getSTSEndpoint());

        AuthUtils.setEnvironmentSTSRegion("cn-beijing");
        provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKey("test")
                .build();
        Assert.assertEquals("sts.cn-beijing." + Config.ENDPOINT_SUFFIX, provider.getSTSEndpoint());

        provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKeyFile(file)
                .stsRegionId("cn-hangzhou")
                .build();
        Assert.assertEquals("sts.cn-hangzhou." + Config.ENDPOINT_SUFFIX, provider.getSTSEndpoint());

        AuthUtils.enableVpcEndpoint(true);
        provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKeyFile(file)
                .stsRegionId("cn-hangzhou")
                .build();
        Assert.assertEquals("sts-vpc.cn-hangzhou." + Config.ENDPOINT_SUFFIX, provider.getSTSEndpoint());

        provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKeyFile(file)
                .stsRegionId("cn-hangzhou")
                .enableVpc(true)
                .build();
        Assert.assertEquals("sts-vpc.cn-hangzhou." + Config.ENDPOINT_SUFFIX, provider.getSTSEndpoint());

        provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKeyFile(file)
                .stsEndpoint("sts.cn-shanghai." + Config.ENDPOINT_SUFFIX)
                .stsRegionId("cn-hangzhou")
                .enableVpc(true)
                .build();
        Assert.assertEquals("sts.cn-shanghai." + Config.ENDPOINT_SUFFIX, provider.getSTSEndpoint());

        provider = RsaKeyPairCredentialProvider.builder()
                .publicKeyId("test")
                .privateKeyFile(file)
                .durationSeconds(1000)
                .stsEndpoint("sts." + Config.ENDPOINT_SUFFIX)
                .connectionTimeout(2000)
                .readTimeout(2000)
                .build();
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals(1000, provider.getDurationSeconds());
        Assert.assertEquals("test", provider.getPublicKeyId());
        Assert.assertEquals("sts." + Config.ENDPOINT_SUFFIX, provider.getSTSEndpoint());

        AuthUtils.setEnvironmentSTSRegion(null);
        AuthUtils.enableVpcEndpoint(false);
        provider.close();
    }
}
