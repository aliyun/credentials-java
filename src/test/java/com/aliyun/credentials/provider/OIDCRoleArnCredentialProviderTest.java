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

public class OIDCRoleArnCredentialProviderTest {

    @Test
    public void createCredentialTest() {
        OIDCRoleArnCredentialProvider provider = OIDCRoleArnCredentialProvider.builder()
                .roleArn("test")
                .oidcProviderArn("test")
                .oidcTokenFilePath(OIDCRoleArnCredentialProviderTest.class.getClassLoader().
                        getResource("OIDCToken.txt").getPath())
                .build();
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
        OIDCRoleArnCredentialProvider provider = OIDCRoleArnCredentialProvider.builder()
                .roleArn("test")
                .oidcProviderArn("test")
                .oidcTokenFilePath(filePath)
                .build();
        provider.setConnectTimeout(888);
        Assert.assertEquals(888, provider.getConnectTimeout());

        provider.setReadTimeout(999);
        Assert.assertEquals(999, provider.getReadTimeout());

        provider.setPolicy("test");
        Assert.assertEquals("test", provider.getPolicy());

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
        Assert.assertEquals("sts." + Config.ENDPOINT_SUFFIX, provider.getSTSEndpoint());

        AuthUtils.setEnvironmentSTSRegion("cn-beijing");
        provider = OIDCRoleArnCredentialProvider.builder()
                .roleArn("test")
                .oidcProviderArn("test")
                .oidcTokenFilePath("OIDCToken.txt")
                .build();
        Assert.assertEquals("sts.cn-beijing." + Config.ENDPOINT_SUFFIX, provider.getSTSEndpoint());

        provider = OIDCRoleArnCredentialProvider.builder()
                .roleArn("test")
                .oidcProviderArn("test")
                .oidcTokenFilePath("OIDCToken.txt")
                .stsRegionId("cn-hangzhou")
                .build();
        Assert.assertEquals("sts.cn-hangzhou." + Config.ENDPOINT_SUFFIX, provider.getSTSEndpoint());

        AuthUtils.enableVpcEndpoint(true);
        provider = OIDCRoleArnCredentialProvider.builder()
                .roleArn("test")
                .oidcProviderArn("test")
                .oidcTokenFilePath("OIDCToken.txt")
                .stsRegionId("cn-hangzhou")
                .build();
        Assert.assertEquals("sts-vpc.cn-hangzhou." + Config.ENDPOINT_SUFFIX, provider.getSTSEndpoint());

        provider = OIDCRoleArnCredentialProvider.builder()
                .roleArn("test")
                .oidcProviderArn("test")
                .oidcTokenFilePath("OIDCToken.txt")
                .stsRegionId("cn-hangzhou")
                .enableVpc(true)
                .build();
        Assert.assertEquals("sts-vpc.cn-hangzhou." + Config.ENDPOINT_SUFFIX, provider.getSTSEndpoint());

        provider = OIDCRoleArnCredentialProvider.builder()
                .roleArn("test")
                .oidcProviderArn("test")
                .oidcTokenFilePath("OIDCToken.txt")
                .stsEndpoint("sts.cn-shanghai." + Config.ENDPOINT_SUFFIX)
                .stsRegionId("cn-hangzhou")
                .enableVpc(true)
                .build();
        Assert.assertEquals("sts.cn-shanghai." + Config.ENDPOINT_SUFFIX, provider.getSTSEndpoint());

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
                .stsEndpoint("sts." + Config.ENDPOINT_SUFFIX)
                .connectionTimeout(2000)
                .readTimeout(2000)
                .build();
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals(1000, provider.getDurationSeconds());
        Assert.assertEquals("test", provider.getRoleArn());
        Assert.assertEquals("test", provider.getRoleSessionName());
        Assert.assertEquals("test", provider.getPolicy());
        Assert.assertEquals("sts." + Config.ENDPOINT_SUFFIX, provider.getSTSEndpoint());
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Error refreshing credentials from OIDC, HttpCode: 400"));
        }
        provider.close();
    }

}