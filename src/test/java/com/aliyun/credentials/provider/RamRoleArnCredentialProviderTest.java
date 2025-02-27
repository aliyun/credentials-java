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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RamRoleArnCredentialProviderTest {

    @Test
    public void getCredentials() {
        RamRoleArnCredentialProvider provider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("test")
                .roleArn("test")
                .roleSessionName("test")
                .connectionTimeout(2000)
                .readTimeout(2000)
                .build();
        provider.setPolicy("test");
        provider.setExternalId("test");
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
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
        RamRoleArnCredentialProvider provider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("test")
                .roleArn("test")
                .build();;
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
        RamRoleArnCredentialProvider provider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("test")
                .roleArn("test")
                .build();;
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

        provider.setSTSEndpoint("www.example.com");
        Assert.assertEquals("www.example.com", provider.getSTSEndpoint());
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
        Assert.assertEquals("sts." + Config.ENDPOINT_SUFFIX, originalProvider.getSTSEndpoint());

        AuthUtils.setEnvironmentSTSRegion("cn-beijing");
        originalProvider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("test")
                .roleArn("test")
                .build();
        Assert.assertEquals("sts.cn-beijing." + Config.ENDPOINT_SUFFIX, originalProvider.getSTSEndpoint());

        originalProvider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("test")
                .stsRegionId("cn-hangzhou")
                .roleArn("test")
                .build();
        Assert.assertEquals("sts.cn-hangzhou." + Config.ENDPOINT_SUFFIX, originalProvider.getSTSEndpoint());

        AuthUtils.enableVpcEndpoint(true);
        originalProvider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("test")
                .stsRegionId("cn-hangzhou")
                .roleArn("test")
                .build();
        Assert.assertEquals("sts-vpc.cn-hangzhou." + Config.ENDPOINT_SUFFIX, originalProvider.getSTSEndpoint());

        originalProvider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("test")
                .stsRegionId("cn-hangzhou")
                .enableVpc(true)
                .roleArn("test")
                .build();
        Assert.assertEquals("sts-vpc.cn-hangzhou." + Config.ENDPOINT_SUFFIX, originalProvider.getSTSEndpoint());

        originalProvider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("test")
                .stsEndpoint("sts.cn-shanghai." + Config.ENDPOINT_SUFFIX)
                .stsRegionId("cn-hangzhou")
                .enableVpc(true)
                .roleArn("test")
                .build();
        Assert.assertEquals("sts.cn-shanghai." + Config.ENDPOINT_SUFFIX, originalProvider.getSTSEndpoint());

        originalProvider = RamRoleArnCredentialProvider.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .durationSeconds(1000)
                .roleArn("test")
                .roleSessionName("test")
                .policy("test")
                .stsEndpoint("sts.cn-hangzhou." + Config.ENDPOINT_SUFFIX)
                .externalId("test")
                .connectionTimeout(2000)
                .readTimeout(2000)
                .build();
        Assert.assertEquals(2000, originalProvider.getConnectTimeout());
        Assert.assertEquals(2000, originalProvider.getReadTimeout());
        Assert.assertEquals(1000, originalProvider.getDurationSeconds());
        Assert.assertEquals("test", originalProvider.getRoleArn());
        Assert.assertEquals("test", originalProvider.getRoleSessionName());
        Assert.assertEquals("test", originalProvider.getPolicy());
        Assert.assertEquals("sts.cn-hangzhou." + Config.ENDPOINT_SUFFIX, originalProvider.getSTSEndpoint());
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
        Assert.assertEquals("sts." + Config.ENDPOINT_SUFFIX, provider.getSTSEndpoint());
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("InvalidAccessKeyId.NotFound"));
        }
        provider.close();
    }

}