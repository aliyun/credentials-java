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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RamRoleArnCredentialProviderTest {

    @Test
    public void constructorTest() {
        RamRoleArnCredentialProvider provider = new RamRoleArnCredentialProvider("id", "secret",
                "name", "arn", "region", "policy");
        Assert.assertEquals("ram_role_arn", provider.getProviderName());
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
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("InvalidAccessKeyId.NotFound"));
        }
    }

    @Test
    public void createCredentialTest() throws NoSuchAlgorithmException, IOException, KeyManagementException {
        Configuration config = new Configuration();
        RamRoleArnCredentialProvider provider = new RamRoleArnCredentialProvider(config);
        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setResponseCode(200);
        response.setHttpContent(new String("{\"Credentials\":{\"Expiration\":\"2019-12-12T1:1:1Z\",\"AccessKeyId\":\"test\"," +
                "\"AccessKeySecret\":\"test\",\"SecurityToken\":\"test\"}}").getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        Assert.assertEquals(AuthConstant.RAM_ROLE_ARN, provider.createCredential(client).value().getType());
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
        RamRoleArnCredentialProvider originalProvider;
        try {
            RamRoleArnCredentialProvider.builder().build();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("RoleArn or environment variable ALIBABA_CLOUD_ROLE_ARN cannot be empty.", e.getMessage());
        }

        try {
            RamRoleArnCredentialProvider.builder()
                    .durationSeconds(100)
                    .build();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("Session duration should be in the range of 900s - max session duration.", e.getMessage());
        }

        originalProvider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("test")
                .roleArn("test")
                .build();
        Assert.assertEquals("sts.aliyuncs.com", originalProvider.getSTSEndpoint());

        AuthUtils.setEnvironmentSTSRegion("cn-beijing");
        originalProvider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("test")
                .roleArn("test")
                .build();
        Assert.assertEquals("sts.cn-beijing.aliyuncs.com", originalProvider.getSTSEndpoint());

        originalProvider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("test")
                .stsRegionId("cn-hangzhou")
                .roleArn("test")
                .build();
        Assert.assertEquals("sts.cn-hangzhou.aliyuncs.com", originalProvider.getSTSEndpoint());

        AuthUtils.enableVpcEndpoint(true);
        originalProvider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("test")
                .stsRegionId("cn-hangzhou")
                .roleArn("test")
                .build();
        Assert.assertEquals("sts-vpc.cn-hangzhou.aliyuncs.com", originalProvider.getSTSEndpoint());

        originalProvider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("test")
                .stsRegionId("cn-hangzhou")
                .enableVpc(true)
                .roleArn("test")
                .build();
        Assert.assertEquals("sts-vpc.cn-hangzhou.aliyuncs.com", originalProvider.getSTSEndpoint());

        originalProvider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("test")
                .STSEndpoint("sts.cn-shanghai.aliyuncs.com")
                .stsRegionId("cn-hangzhou")
                .enableVpc(true)
                .roleArn("test")
                .build();
        Assert.assertEquals("sts.cn-shanghai.aliyuncs.com", originalProvider.getSTSEndpoint());

        originalProvider = RamRoleArnCredentialProvider.builder()
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
        Assert.assertEquals(2000, originalProvider.getConnectTimeout());
        Assert.assertEquals(2000, originalProvider.getReadTimeout());
        Assert.assertEquals(1000, originalProvider.getDurationSeconds());
        Assert.assertEquals("test", originalProvider.getAccessKeyId());
        Assert.assertEquals("test", originalProvider.getAccessKeySecret());
        Assert.assertEquals("test", originalProvider.getRoleArn());
        Assert.assertEquals("test", originalProvider.getRoleSessionName());
        Assert.assertEquals("test", originalProvider.getPolicy());
        Assert.assertEquals("sts.cn-hangzhou.aliyuncs.com", originalProvider.getSTSEndpoint());
        Assert.assertEquals("cn-hangzhou", originalProvider.getRegionId());
        Assert.assertEquals("test", originalProvider.getExternalId());
        try {
            originalProvider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("InvalidAccessKeyId.NotFound"));
        }

        AuthUtils.setEnvironmentSTSRegion(null);
        AuthUtils.enableVpcEndpoint(false);

        RamRoleArnCredentialProvider provider = RamRoleArnCredentialProvider.builder()
                .credentialsProvider(originalProvider)
                .durationSeconds(1000)
                .roleArn("test")
                .build();
        Assert.assertEquals("test", provider.getRoleArn());
        Assert.assertTrue(provider.getRoleSessionName().contains("credentials-java-"));
        Assert.assertEquals("sts.aliyuncs.com", provider.getSTSEndpoint());
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("InvalidAccessKeyId.NotFound"));
        }
        provider.close();
    }

}