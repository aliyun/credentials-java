package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.FormatType;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.models.CredentialModel;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CloudSSOCredentialsProviderTest {

    @Test
    public void testBuilderValidation() {
        try {
            CloudSSOCredentialsProvider.builder()
                    .signInUrl("https://signin.aliyuncs.com")
                    .accountId("123456")
                    .accessConfig("ac-config")
                    .accessToken("")
                    .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                    .build();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("CloudSSO access token is empty or expired, please re-login with cli.", e.getMessage());
        }

        try {
            CloudSSOCredentialsProvider.builder()
                    .signInUrl("https://signin.aliyuncs.com")
                    .accountId("123456")
                    .accessConfig("ac-config")
                    .accessToken("token")
                    .accessTokenExpire(0)
                    .build();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("CloudSSO access token is empty or expired, please re-login with cli.", e.getMessage());
        }

        try {
            CloudSSOCredentialsProvider.builder()
                    .signInUrl("https://signin.aliyuncs.com")
                    .accountId("123456")
                    .accessConfig("ac-config")
                    .accessToken("token")
                    .accessTokenExpire(1)
                    .build();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("CloudSSO access token is empty or expired, please re-login with cli.", e.getMessage());
        }

        try {
            CloudSSOCredentialsProvider.builder()
                    .accountId("123456")
                    .accessConfig("ac-config")
                    .accessToken("token")
                    .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                    .build();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("CloudSSO sign in url, account id, and access config cannot be empty.", e.getMessage());
        }

        try {
            CloudSSOCredentialsProvider.builder()
                    .signInUrl("https://signin.aliyuncs.com")
                    .accessConfig("ac-config")
                    .accessToken("token")
                    .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                    .build();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("CloudSSO sign in url, account id, and access config cannot be empty.", e.getMessage());
        }

        try {
            CloudSSOCredentialsProvider.builder()
                    .signInUrl("https://signin.aliyuncs.com")
                    .accountId("123456")
                    .accessToken("token")
                    .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                    .build();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("CloudSSO sign in url, account id, and access config cannot be empty.", e.getMessage());
        }
    }

    @Test
    public void testGetNewSessionCredentials() {
        CloudSSOCredentialsProvider provider = CloudSSOCredentialsProvider.builder()
                .signInUrl("https://signin.aliyuncs.com")
                .accountId("123456")
                .accessConfig("ac-config")
                .accessToken("valid-token")
                .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                .build();

        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setResponseCode(200);
        response.setHttpContent(("{\"CloudCredential\":{\"AccessKeyId\":\"ak\",\"AccessKeySecret\":\"sk\"," +
                "\"SecurityToken\":\"token\",\"Expiration\":\"2019-12-12T1:1:1Z\"}}").getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);

        RefreshResult<CredentialModel> result = provider.getNewSessionCredentials(client);
        Assert.assertNotNull(result);
        CredentialModel credential = result.value();
        Assert.assertEquals("ak", credential.getAccessKeyId());
        Assert.assertEquals("sk", credential.getAccessKeySecret());
        Assert.assertEquals("token", credential.getSecurityToken());
        provider.close();
    }

    @Test
    public void testGetNewSessionCredentialsError() {
        CloudSSOCredentialsProvider provider = CloudSSOCredentialsProvider.builder()
                .signInUrl("https://signin.aliyuncs.com")
                .accountId("123456")
                .accessConfig("ac-config")
                .accessToken("valid-token")
                .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                .build();

        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setResponseCode(400);
        response.setHttpContent("Bad Request".getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);

        try {
            provider.getNewSessionCredentials(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("Get session token from CloudSSO failed, HttpCode: 400"));
        }
        provider.close();
    }

    @Test
    public void testGetNewSessionCredentialsInvalidJson() {
        CloudSSOCredentialsProvider provider = CloudSSOCredentialsProvider.builder()
                .signInUrl("https://signin.aliyuncs.com")
                .accountId("123456")
                .accessConfig("ac-config")
                .accessToken("valid-token")
                .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                .build();

        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setResponseCode(200);
        response.setHttpContent("{\"invalid\":\"response\"}".getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);

        try {
            provider.getNewSessionCredentials(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("Get session token from CloudSSO failed"));
        }
        provider.close();
    }

    @Test
    public void testGetProviderName() {
        CloudSSOCredentialsProvider provider = CloudSSOCredentialsProvider.builder()
                .signInUrl("https://signin.aliyuncs.com")
                .accountId("123456")
                .accessConfig("ac-config")
                .accessToken("valid-token")
                .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                .build();
        Assert.assertEquals("cloud_sso", provider.getProviderName());
        provider.close();
    }

    @Test
    public void testBuilder() {
        long expire = System.currentTimeMillis() / 1000 + 7200;
        CloudSSOCredentialsProvider provider = CloudSSOCredentialsProvider.builder()
                .signInUrl("https://signin.example.com")
                .accountId("account-123")
                .accessConfig("config-456")
                .accessToken("my-token")
                .accessTokenExpire(expire)
                .connectTimeout(3000)
                .readTimeout(6000)
                .build();

        Assert.assertNotNull(provider);
        Assert.assertEquals("cloud_sso", provider.getProviderName());
        provider.close();
    }
}
