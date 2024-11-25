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

public class OIDCRoleArnCredentialProviderTest {

    @Test
    public void constructorTest() {
        OIDCRoleArnCredentialProvider provider;
        try {
            provider = new OIDCRoleArnCredentialProvider("", "", "");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("com.aliyun.credentials.exception.CredentialException: " +
                            "roleArn does not exist and env ALIBABA_CLOUD_ROLE_ARN is null.",
                    e.toString());
        }
        try {
            provider = new OIDCRoleArnCredentialProvider("arn", "", "");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("com.aliyun.credentials.exception.CredentialException: " +
                            "OIDCProviderArn does not exist and env ALIBABA_CLOUD_OIDC_PROVIDER_ARN is null.",
                    e.toString());
        }
        try {
            provider = new OIDCRoleArnCredentialProvider("id", "secret",
                    "name", "arn", "providerArn", "", "region", "policy");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("com.aliyun.credentials.exception.CredentialException: " +
                            "OIDCTokenFilePath does not exist and env ALIBABA_CLOUD_OIDC_TOKEN_FILE is null.",
                    e.toString());
        }
        String filePath = OIDCRoleArnCredentialProviderTest.class.getClassLoader().
                getResource("OIDCToken.txt").getPath();
        provider = new OIDCRoleArnCredentialProvider("id", "secret",
                "name", "arn", "providerArn", filePath, "region", "policy");
        Assert.assertEquals("oidc_role_arn", provider.getProviderName());
        Assert.assertEquals("name", provider.getRoleSessionName());
        Assert.assertEquals("region", provider.getRegionId());
        Assert.assertEquals("policy", provider.getPolicy());
        Assert.assertEquals("arn", provider.getRoleArn());
        Assert.assertEquals("providerArn", provider.getOIDCProviderArn());
        Assert.assertTrue(provider.getOIDCTokenFilePath().contains("OIDCToken.txt"));

        Configuration config = new Configuration();
        config.setAccessKeyId("test");
        config.setAccessKeySecret("test");
        config.setRoleArn("test");
        config.setOIDCProviderArn("test");
        config.setRoleSessionName("test");
        config.setConnectTimeout(2000);
        config.setReadTimeout(2000);
        config.setOIDCTokenFilePath(filePath);
        provider = new OIDCRoleArnCredentialProvider(config);
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals("test", provider.getRoleArn());
        Assert.assertEquals("test", provider.getOIDCProviderArn());
        Assert.assertEquals("test", provider.getRoleSessionName());
        Assert.assertNull(provider.getPolicy());
        config.setSTSEndpoint("sts.cn-hangzhou.aliyuncs.com");
        provider = new OIDCRoleArnCredentialProvider(config);
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
        config1.oidcTokenFilePath = filePath;
        provider = new OIDCRoleArnCredentialProvider(config1);
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals(1000, provider.getDurationSeconds());
        Assert.assertEquals("test", provider.getRoleArn());
        Assert.assertEquals("test", provider.getOIDCProviderArn());
        Assert.assertEquals("test", provider.getRoleSessionName());
        Assert.assertEquals("test", provider.getPolicy());
        config1.STSEndpoint = "sts.cn-hangzhou.aliyuncs.com";
        provider = new OIDCRoleArnCredentialProvider(config);
        Assert.assertEquals("sts.cn-hangzhou.aliyuncs.com", provider.getSTSEndpoint());
    }

    @Test
    public void getCredentials() {
        Configuration config = new Configuration();
        config.setAccessKeyId("test");
        config.setAccessKeySecret("test");
        config.setRoleArn("test");
        config.setOIDCProviderArn("test");
        config.setRoleSessionName("test");
        config.setConnectTimeout(2000);
        config.setReadTimeout(2000);
        OIDCRoleArnCredentialProvider provider;
        try {
            provider = new OIDCRoleArnCredentialProvider(config);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("com.aliyun.credentials.exception.CredentialException: OIDCTokenFilePath does not exist and env ALIBABA_CLOUD_OIDC_TOKEN_FILE is null.",
                    e.toString());
        }
        config.setOIDCTokenFilePath(OIDCRoleArnCredentialProviderTest.class.getClassLoader().
                getResource("OIDCToken.txt").getPath());
        provider = new OIDCRoleArnCredentialProvider(config);
        provider.setPolicy("test");
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals("test", provider.getRoleArn());
        Assert.assertEquals("test", provider.getOIDCProviderArn());
        Assert.assertEquals("test", provider.getRoleSessionName());
        Assert.assertTrue(provider.getOIDCTokenFilePath().contains("OIDCToken.txt"));
        Assert.assertNull(provider.getOIDCToken());
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Parameter OIDCProviderArn is not valid"));
            Assert.assertTrue(e.getMessage().contains("AuthenticationFail.NoPermission"));
        }
        Assert.assertEquals("OIDCToken", provider.getOIDCToken());
    }

    @Test
    public void createCredentialTest() {
        Configuration config = new Configuration();
        config.setRoleArn("test");
        config.setOIDCProviderArn("test");
        config.setOIDCTokenFilePath(OIDCRoleArnCredentialProviderTest.class.getClassLoader().
                getResource("OIDCToken.txt").getPath());
        OIDCRoleArnCredentialProvider provider = new OIDCRoleArnCredentialProvider(config);
        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setResponseCode(200);
        response.setHttpContent(new String("{\"Credentials\":{\"Expiration\":\"2019-12-12T1:1:1Z\",\"AccessKeyId\":\"test\"," +
                "\"AccessKeySecret\":\"test\",\"SecurityToken\":\"test\"}}").getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        Assert.assertEquals(AuthConstant.OIDC_ROLE_ARN, provider.createCredential(client).value().getType());
    }

    @Test
    public void getSetTest() {
        String filePath = OIDCRoleArnCredentialProviderTest.class.getClassLoader().
                getResource("OIDCToken.txt").getPath();
        OIDCRoleArnCredentialProvider provider = new OIDCRoleArnCredentialProvider("test", "test", filePath);
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

        provider.setSTSEndpoint("www.aliyun.com");
        Assert.assertEquals("www.aliyun.com", provider.getSTSEndpoint());
    }

    @Test
    public void builderTest() {
        OIDCRoleArnCredentialProvider provider;
        try {
            OIDCRoleArnCredentialProvider.builder().build();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("RoleArn or environment variable ALIBABA_CLOUD_ROLE_ARN cannot be empty.", e.getMessage());
        }

        try {
            OIDCRoleArnCredentialProvider.builder()
                    .roleArn("test")
                    .build();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("OIDCProviderArn or environment variable ALIBABA_CLOUD_OIDC_PROVIDER_ARN cannot be empty.", e.getMessage());
        }

        try {
            OIDCRoleArnCredentialProvider.builder()
                    .roleArn("test")
                    .oidcProviderArn("test")
                    .build();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("OIDCTokenFilePath or environment variable ALIBABA_CLOUD_OIDC_TOKEN_FILE cannot be empty.", e.getMessage());
        }

        try {
            OIDCRoleArnCredentialProvider.builder()
                    .durationSeconds(100)
                    .build();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("Session duration should be in the range of 900s - max session duration.", e.getMessage());
        }

        provider = OIDCRoleArnCredentialProvider.builder()
                .roleArn("test")
                .oidcProviderArn("test")
                .oidcTokenFilePath("OIDCToken.txt")
                .build();
        Assert.assertEquals("sts.aliyuncs.com", provider.getSTSEndpoint());

        AuthUtils.setEnvironmentSTSRegion("cn-beijing");
        provider = OIDCRoleArnCredentialProvider.builder()
                .roleArn("test")
                .oidcProviderArn("test")
                .oidcTokenFilePath("OIDCToken.txt")
                .build();
        Assert.assertEquals("sts.cn-beijing.aliyuncs.com", provider.getSTSEndpoint());

        provider = OIDCRoleArnCredentialProvider.builder()
                .roleArn("test")
                .oidcProviderArn("test")
                .oidcTokenFilePath("OIDCToken.txt")
                .stsRegionId("cn-hangzhou")
                .build();
        Assert.assertEquals("sts.cn-hangzhou.aliyuncs.com", provider.getSTSEndpoint());

        AuthUtils.enableVpcEndpoint(true);
        provider = OIDCRoleArnCredentialProvider.builder()
                .roleArn("test")
                .oidcProviderArn("test")
                .oidcTokenFilePath("OIDCToken.txt")
                .stsRegionId("cn-hangzhou")
                .build();
        Assert.assertEquals("sts-vpc.cn-hangzhou.aliyuncs.com", provider.getSTSEndpoint());

        provider = OIDCRoleArnCredentialProvider.builder()
                .roleArn("test")
                .oidcProviderArn("test")
                .oidcTokenFilePath("OIDCToken.txt")
                .stsRegionId("cn-hangzhou")
                .enableVpc(true)
                .build();
        Assert.assertEquals("sts-vpc.cn-hangzhou.aliyuncs.com", provider.getSTSEndpoint());

        provider = OIDCRoleArnCredentialProvider.builder()
                .roleArn("test")
                .oidcProviderArn("test")
                .oidcTokenFilePath("OIDCToken.txt")
                .STSEndpoint("sts.cn-shanghai.aliyuncs.com")
                .stsRegionId("cn-hangzhou")
                .enableVpc(true)
                .build();
        Assert.assertEquals("sts.cn-shanghai.aliyuncs.com", provider.getSTSEndpoint());

        AuthUtils.setEnvironmentSTSRegion(null);
        AuthUtils.enableVpcEndpoint(false);

        String filePath = OIDCRoleArnCredentialProviderTest.class.getClassLoader().
                getResource("OIDCToken.txt").getPath();
        provider = OIDCRoleArnCredentialProvider.builder()
                .roleArn("test")
                .oidcProviderArn("test")
                .oidcTokenFilePath(filePath)
                .durationSeconds(1000)
                .roleSessionName("test")
                .policy("test")
                .STSEndpoint("sts.aliyuncs.com")
                .regionId("cn-hangzhou")
                .connectionTimeout(2000)
                .readTimeout(2000)
                .build();
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals(1000, provider.getDurationSeconds());
        Assert.assertEquals("test", provider.getRoleArn());
        Assert.assertEquals("test", provider.getRoleSessionName());
        Assert.assertEquals("test", provider.getPolicy());
        Assert.assertEquals("sts.aliyuncs.com", provider.getSTSEndpoint());
        Assert.assertEquals("cn-hangzhou", provider.getRegionId());
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Error refreshing credentials from OIDC, HttpCode: 400"));
        }
        provider.close();
    }

}