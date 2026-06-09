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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OAuthCredentialsProviderTest {

    @Test
    public void testBuilderValidation() {
        try {
            OAuthCredentialsProvider.builder()
                    .clientId("")
                    .signInUrl("https://oauth.aliyun.com")
                    .build();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("The clientId is empty.", e.getMessage());
        }

        try {
            OAuthCredentialsProvider.builder()
                    .clientId("client-id")
                    .signInUrl("")
                    .build();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("The url for sign-in is empty.", e.getMessage());
        }

        try {
            OAuthCredentialsProvider.builder()
                    .clientId("client-id")
                    .build();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("The url for sign-in is empty.", e.getMessage());
        }
    }

    @Test
    public void testGetNewSessionCredentials() {
        OAuthCredentialsProvider provider = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("https://oauth.aliyun.com")
                .accessToken("valid-access-token")
                .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                .build();

        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setResponseCode(200);
        response.setHttpContent(("{\"accessKeyId\":\"ak\",\"accessKeySecret\":\"sk\"," +
                "\"securityToken\":\"token\",\"expiration\":\"2019-12-12T1:1:1Z\"}").getBytes(), "UTF-8", FormatType.JSON);
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
        OAuthCredentialsProvider provider = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("https://oauth.aliyun.com")
                .accessToken("valid-access-token")
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
            Assert.assertTrue(e.getMessage().contains("Get session token from OAuth failed, HttpCode: 400"));
        }
        provider.close();
    }

    @Test
    public void testGetNewSessionCredentialsInvalidJson() {
        OAuthCredentialsProvider provider = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("https://oauth.aliyun.com")
                .accessToken("valid-access-token")
                .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                .build();

        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setResponseCode(200);
        response.setHttpContent("not json at all".getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);

        try {
            provider.getNewSessionCredentials(client);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertNotNull(e.getMessage());
        }
        provider.close();
    }

    @Test
    public void testGetNewSessionCredentialsMissingFields() {
        OAuthCredentialsProvider provider = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("https://oauth.aliyun.com")
                .accessToken("valid-access-token")
                .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                .build();

        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setResponseCode(200);
        response.setHttpContent(("{\"accessKeyId\":\"\",\"accessKeySecret\":\"sk\"," +
                "\"securityToken\":\"token\",\"expiration\":\"2019-12-12T1:1:1Z\"}").getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);

        try {
            provider.getNewSessionCredentials(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("Refresh session token from OAuth failed, fail to get credentials"));
        }
        provider.close();
    }

    @Test
    public void testTryRefreshOAuthToken() {
        OAuthCredentialsProvider provider = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("https://oauth.aliyun.com")
                .refreshToken("old-refresh-token")
                .accessToken("expired-token")
                .accessTokenExpire(0)
                .build();

        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);

        HttpResponse refreshResponse = new HttpResponse("test?test=test");
        refreshResponse.setResponseCode(200);
        refreshResponse.setHttpContent(("{\"access_token\":\"new\",\"refresh_token\":\"new_refresh\"," +
                "\"expires_in\":3600}").getBytes(), "UTF-8", FormatType.JSON);

        HttpResponse exchangeResponse = new HttpResponse("test?test=test");
        exchangeResponse.setResponseCode(200);
        exchangeResponse.setHttpContent(("{\"accessKeyId\":\"ak\",\"accessKeySecret\":\"sk\"," +
                "\"securityToken\":\"token\",\"expiration\":\"2019-12-12T1:1:1Z\"}").getBytes(), "UTF-8", FormatType.JSON);

        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any()))
                .thenReturn(refreshResponse)
                .thenReturn(exchangeResponse);

        RefreshResult<CredentialModel> result = provider.getNewSessionCredentials(client);
        Assert.assertNotNull(result);
        CredentialModel credential = result.value();
        Assert.assertEquals("ak", credential.getAccessKeyId());
        Assert.assertEquals("sk", credential.getAccessKeySecret());
        Assert.assertEquals("token", credential.getSecurityToken());
        provider.close();
    }

    @Test
    public void testTryRefreshOAuthTokenError() {
        OAuthCredentialsProvider provider = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("https://oauth.aliyun.com")
                .refreshToken("old-refresh-token")
                .accessToken("expired-token")
                .accessTokenExpire(0)
                .build();

        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setResponseCode(400);
        response.setHttpContent("token refresh failed".getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);

        try {
            provider.getNewSessionCredentials(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("Failed to refresh OAuth token, status code: 400"));
        }
        provider.close();
    }

    @Test
    public void testTokenUpdateCallback() {
        final AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        final AtomicReference<String> capturedAk = new AtomicReference<>();

        OAuthCredentialsProvider.OAuthTokenUpdateCallback callback =
                (refreshToken, accessToken, accessKeyId, accessKeySecret, securityToken, accessTokenExpire, stsExpire) -> {
                    callbackInvoked.set(true);
                    capturedAk.set(accessKeyId);
                };

        OAuthCredentialsProvider provider = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("https://oauth.aliyun.com")
                .accessToken("valid-access-token")
                .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                .tokenUpdateCallback(callback)
                .build();

        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setResponseCode(200);
        response.setHttpContent(("{\"accessKeyId\":\"ak\",\"accessKeySecret\":\"sk\"," +
                "\"securityToken\":\"token\",\"expiration\":\"2019-12-12T1:1:1Z\"}").getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);

        provider.getNewSessionCredentials(client);
        Assert.assertTrue(callbackInvoked.get());
        Assert.assertEquals("ak", capturedAk.get());
        provider.close();
    }

    @Test
    public void testGetProviderName() {
        OAuthCredentialsProvider provider = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("https://oauth.aliyun.com")
                .accessToken("token")
                .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                .build();
        Assert.assertEquals("oauth", provider.getProviderName());
        provider.close();
    }
}
