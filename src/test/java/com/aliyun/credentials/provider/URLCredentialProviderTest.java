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

import java.net.MalformedURLException;
import java.net.URL;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class URLCredentialProviderTest {

    @Test
    public void constructorTest() throws MalformedURLException {
        URLCredentialProvider provider;
        try {
            new URLCredentialProvider();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("com.aliyun.credentials.exception.CredentialException: Credential URI cannot be null.",
                    e.toString());
        }
        try {
            new URLCredentialProvider("");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("com.aliyun.credentials.exception.CredentialException: Credential URI cannot be null.",
                    e.toString());
        }
        try {
            new URLCredentialProvider((URL) null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("com.aliyun.credentials.exception.CredentialException: Credential URI cannot be null.",
                    e.toString());
        }
        try {
            new URLCredentialProvider("url");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("com.aliyun.credentials.exception.CredentialException: Credential URI is not valid.",
                    e.toString());
        }

        provider = new URLCredentialProvider(new URL("http://test"));
        Assert.assertEquals("credentials_uri", provider.getProviderName());
        Assert.assertEquals("http://test", provider.getURL());

        provider = new URLCredentialProvider("http://test");
        Assert.assertEquals("http://test", provider.getURL());

        provider = URLCredentialProvider.builder()
                .credentialsURI("http://test")
                .build();
        Assert.assertEquals("http://test", provider.getURL());

        provider = URLCredentialProvider.builder()
                .credentialsURI(new URL("http://test"))
                .build();
        Assert.assertEquals("http://test", provider.getURL());
        provider.close();

        Config config = new Config();
        config.setCredentialsUri("url");
        config.setConnectTimeout(2000);
        config.setTimeout(2000);
        try {
            new URLCredentialProvider(config);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("com.aliyun.credentials.exception.CredentialException: Credential URI is not valid.",
                    e.toString());
        }

        try {
            URLCredentialProvider.builder().build();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("Credential URI or environment variable ALIBABA_CLOUD_CREDENTIALS_URI cannot be empty.",
                    e.getMessage());
        }

        provider = URLCredentialProvider.builder()
                .credentialsURI(new URL("http://test1"))
                .credentialsURI("http://test2")
                .readTimeout(2000)
                .connectionTimeout(2000)
                .build();
        Assert.assertEquals("credentials_uri", provider.getProviderName());
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals("http://test2", provider.getURL());
    }

    @Test
    public void getCredentials() {
        Configuration config = new Configuration();
        config.setAccessKeyId("test");
        config.setAccessKeySecret("test");
        config.setCredentialsURI("url");
        config.setConnectTimeout(2000);
        config.setReadTimeout(2000);
        URLCredentialProvider provider;
        try {
            new URLCredentialProvider("url");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("com.aliyun.credentials.exception.CredentialException: Credential URI is not valid.",
                    e.toString());
        }
        config.setCredentialsURI("http://10.10.10.10");
        provider = new URLCredentialProvider(config);
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals("http://10.10.10.10", provider.getURL());
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Failed to connect Server:")
                    || e.getMessage().contains("Failed to get credentials from server: http://10.10.10.10"));
            Assert.assertFalse(e.getMessage().contains("HttpCode: 0"));
        } finally {
            provider.close();
        }

        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setResponseCode(200);
        response.setHttpContent(("{\"Code\":\"Success\",  \"AccessKeyId\":\"test\", " +
                        "\"AccessKeySecret\":\"test\", \"SecurityToken\":\"test\",  \"Expiration\":\"2019-08-08T1:1:1Z\"}").getBytes(),
                "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        Assert.assertEquals(AuthConstant.CREDENTIALS_URI, provider.getNewSessionCredentials(client).value().getType());

        response = new HttpResponse("test?test=test");
        response.setResponseCode(403);
        response.setResponseMessage("Forbidden");
        response.setHttpContent("{\"Code\":\"Denied\"}".getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        try {
            provider.getNewSessionCredentials(client);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Failed to get credentials from server: http://10.10.10.10"));
            Assert.assertTrue(e.getMessage().contains("HttpCode: 403"));
            Assert.assertTrue(e.getMessage().contains("ResponseMessage: Forbidden"));
            Assert.assertTrue(e.getMessage().contains("Denied"));
        }

        response = new HttpResponse("test?test=test");
        response.setResponseCode(200);
        response.setHttpContent("test".getBytes(),
                "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        try {
            provider.getNewSessionCredentials(client);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Failed to get credentials from server: http://10.10.10.10"));
        }
    }

    @Test
    public void getSetTest() {
        URLCredentialProvider provider = new URLCredentialProvider("http://10.10.10.10");
        Assert.assertEquals("http://10.10.10.10", provider.getURL());
        provider.setConnectTimeout(888);
        Assert.assertEquals(888, provider.getConnectTimeout());
        provider.setReadTimeout(999);
        Assert.assertEquals(999, provider.getReadTimeout());

    }

    @Test
    public void configConstructorCopiesTimeouts() {
        Config config = new Config();
        config.credentialsURI = "http://127.0.0.1/credentials";
        config.connectTimeout = 1500;
        config.timeout = 2500;
        URLCredentialProvider provider = new URLCredentialProvider(config);
        Assert.assertEquals("http://127.0.0.1/credentials", provider.getURL());
        Assert.assertEquals(1500, provider.getConnectTimeout());
        Assert.assertEquals(2500, provider.getReadTimeout());
        provider.close();
    }

    @Test
    public void builderRejectsMalformedUri() {
        try {
            URLCredentialProvider.builder().credentialsURI("not a url").build();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Credential URI is not valid.", e.getMessage());
        }
    }

    @Test
    public void getNewSessionCredentialsRejectsIncompletePayload() throws Exception {
        URLCredentialProvider provider = new URLCredentialProvider("http://127.0.0.1/credentials");
        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);

        HttpResponse denied = new HttpResponse("http://127.0.0.1/credentials");
        denied.setResponseCode(200);
        denied.setHttpContent("{\"Code\":\"Failed\"}".getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(denied);
        try {
            provider.getNewSessionCredentials(client);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Error retrieving credentials from credentialsURI result"));
        }

        HttpResponse missing = new HttpResponse("http://127.0.0.1/credentials");
        missing.setResponseCode(200);
        missing.setHttpContent("{\"Code\":\"Success\",\"AccessKeyId\":\"ak\"}".getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(missing);
        try {
            provider.getNewSessionCredentials(client);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Error retrieving credentials from credentialsURI result"));
        }

        HttpResponse empty = new HttpResponse("http://127.0.0.1/credentials");
        empty.setResponseCode(200);
        empty.setHttpContent("null".getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(empty);
        try {
            provider.getNewSessionCredentials(client);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Error retrieving credentials from credentialsURI result"));
        }
        provider.close();
    }

    @Test
    public void refreshCredentialsReadsLocalServer() throws Exception {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/credentials", exchange -> {
            byte[] body = ("{\"Code\":\"Success\",\"AccessKeyId\":\"ak\",\"AccessKeySecret\":\"sk\","
                    + "\"SecurityToken\":\"token\",\"Expiration\":\"2030-01-01T00:00:00Z\"}").getBytes("UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/credentials";
            URLCredentialProvider provider = URLCredentialProvider.builder().credentialsURI(url).build();
            Assert.assertEquals("ak", provider.getCredentials().getAccessKeyId());
            Assert.assertEquals("credentials_uri", provider.getCredentials().getProviderName());
            provider.close();
        } finally {
            server.stop(0);
        }
    }

}