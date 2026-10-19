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
        // Real OAuth /v1/exchange body uses PascalCase fields
        response.setHttpContent(("{\"AccessKeyId\":\"ak\",\"AccessKeySecret\":\"sk\"," +
                "\"SecurityToken\":\"token\",\"Expiration\":\"2019-12-12T1:1:1Z\"}").getBytes(), "UTF-8", FormatType.JSON);
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
    public void testGetNewSessionCredentialsCamelCaseFallback() {
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
        Assert.assertEquals("ak", result.value().getAccessKeyId());
        Assert.assertEquals("sk", result.value().getAccessKeySecret());
        Assert.assertEquals("token", result.value().getSecurityToken());
        provider.close();
    }

    @Test
    public void testGetExchangeFieldPrefersPascalCase() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("AccessKeyId", "pascal");
        map.put("accessKeyId", "camel");
        Assert.assertEquals("pascal", OAuthCredentialsProvider.getExchangeField(map, "AccessKeyId", "accessKeyId"));
        Assert.assertEquals("camel", OAuthCredentialsProvider.getExchangeField(
                java.util.Collections.singletonMap("accessKeyId", "camel"), "AccessKeyId", "accessKeyId"));
        Assert.assertNull(OAuthCredentialsProvider.getExchangeField(
                java.util.Collections.emptyMap(), "AccessKeyId", "accessKeyId"));
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
        exchangeResponse.setHttpContent(("{\"AccessKeyId\":\"ak\",\"AccessKeySecret\":\"sk\"," +
                "\"SecurityToken\":\"token\",\"Expiration\":\"2019-12-12T1:1:1Z\"}").getBytes(), "UTF-8", FormatType.JSON);

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
            Assert.assertTrue(e.getMessage().contains("Failed to refresh OAuth token, HttpCode: 400"));
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
        response.setHttpContent(("{\"AccessKeyId\":\"ak\",\"AccessKeySecret\":\"sk\"," +
                "\"SecurityToken\":\"token\",\"Expiration\":\"2019-12-12T1:1:1Z\"}").getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);

        provider.getNewSessionCredentials(client);
        Assert.assertTrue(callbackInvoked.get());
        Assert.assertEquals("ak", capturedAk.get());
        provider.close();
    }

    @Test
    public void testInvalidSignInUrlAndTransportErrors() {
        OAuthCredentialsProvider provider = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("not a url")
                .accessToken("valid-access-token")
                .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                .build();
        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        try {
            provider.getNewSessionCredentials(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid OAuth sign in url"));
        }

        OAuthCredentialsProvider refresh = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("not a url")
                .refreshToken("refresh")
                .accessTokenExpire(0)
                .build();
        try {
            refresh.getNewSessionCredentials(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid OAuth sign in url"));
        }

        OAuthCredentialsProvider connected = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("https://oauth.aliyun.com")
                .accessToken("valid-access-token")
                .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                .build();
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenThrow(new RuntimeException("boom"));
        try {
            connected.getNewSessionCredentials(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("Failed to connect OAuth service: boom"));
        }

        OAuthCredentialsProvider refreshing = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("https://oauth.aliyun.com")
                .refreshToken("refresh")
                .accessTokenExpire(0)
                .build();
        try {
            refreshing.getNewSessionCredentials(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("Failed to refresh OAuth token: boom"));
        }
        provider.close();
        refresh.close();
        connected.close();
        refreshing.close();
    }

    @Test
    public void testEmptyAndIncompleteTokenResponses() {
        OAuthCredentialsProvider provider = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("https://oauth.aliyun.com")
                .accessToken("valid-access-token")
                .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                .tokenUpdateCallback((refreshToken, accessToken, accessKeyId, accessKeySecret, securityToken,
                                       accessTokenExpire, stsExpire) -> {
                    throw new IllegalStateException("callback failed");
                })
                .build();
        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse success = new HttpResponse("test?test=test");
        success.setResponseCode(200);
        success.setHttpContent(("{\"AccessKeyId\":\"ak\",\"AccessKeySecret\":\"sk\","
                + "\"SecurityToken\":\"token\",\"Expiration\":\"2019-12-12T1:1:1Z\"}").getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(success);
        Assert.assertEquals("ak", provider.getNewSessionCredentials(client).value().getAccessKeyId());

        HttpResponse response = new HttpResponse("test?test=test");
        response.setResponseCode(200);
        response.setHttpContent("null".getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        try {
            provider.getNewSessionCredentials(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("result: null"));
        }

        OAuthCredentialsProvider refreshing = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("https://oauth.aliyun.com")
                .refreshToken("refresh")
                .accessTokenExpire(0)
                .build();
        HttpResponse emptyRefresh = new HttpResponse("test?test=test");
        emptyRefresh.setResponseCode(200);
        emptyRefresh.setHttpContent("null".getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(emptyRefresh);
        try {
            refreshing.getNewSessionCredentials(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("empty response"));
        }

        HttpResponse incomplete = new HttpResponse("test?test=test");
        incomplete.setResponseCode(200);
        incomplete.setHttpContent("{\"expires_in\":120}".getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(incomplete);
        try {
            refreshing.getNewSessionCredentials(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("Failed to refresh OAuth token"));
        }

        HttpResponse noExpires = new HttpResponse("test?test=test");
        noExpires.setResponseCode(200);
        noExpires.setHttpContent("{\"access_token\":\"new\",\"refresh_token\":\"newer\"}".getBytes(), "UTF-8", FormatType.JSON);
        HttpResponse exchange = new HttpResponse("test?test=test");
        exchange.setResponseCode(200);
        exchange.setHttpContent(("{\"AccessKeyId\":\"ak\",\"AccessKeySecret\":\"sk\","
                + "\"SecurityToken\":\"token\",\"Expiration\":\"2019-12-12T1:1:1Z\"}").getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(noExpires).thenReturn(exchange);
        Assert.assertEquals("ak", refreshing.getNewSessionCredentials(client).value().getAccessKeyId());
        provider.close();
        refreshing.close();
    }

    @Test
    public void testGetProviderName() {
        OAuthCredentialsProvider provider = OAuthCredentialsProvider.builder()
                .clientId("test-client")
                .signInUrl("https://oauth.aliyun.com")
                .accessToken("token")
                .accessTokenExpire(System.currentTimeMillis() / 1000 + 3600)
                .connectTimeout(2000)
                .readTimeout(3000)
                .build();
        Assert.assertEquals("oauth", provider.getProviderName());
        provider.close();
    }
}
