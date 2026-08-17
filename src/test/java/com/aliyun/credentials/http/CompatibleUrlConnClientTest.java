package com.aliyun.credentials.http;

import com.aliyun.credentials.exception.CredentialException;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompatibleUrlConnClientTest {
    @Test
    public void staticCompatibleGetResponseTest() {
        HttpRequest httpRequest = new HttpRequest("https://www.aliyun.com");
        httpRequest.setSysMethod(MethodType.GET);
        httpRequest.setSysConnectTimeout(10000);
        httpRequest.setSysReadTimeout(10000);
        HttpResponse response = CompatibleUrlConnClient.compatibleGetResponse(httpRequest);
        Assert.assertNotNull(response);

        httpRequest = new HttpRequest("http://www.aliyun.com");
        httpRequest.setSysMethod(MethodType.GET);
        httpRequest.setSysConnectTimeout(1);
        httpRequest.setSysReadTimeout(1);
        try {
            CompatibleUrlConnClient.compatibleGetResponse(httpRequest);
            Assert.fail("transport failure should throw CredentialException");
        } catch (CredentialException e) {
            Assert.assertNotNull(e.getMessage());
            Assert.assertTrue(e.getMessage().toLowerCase(Locale.ROOT).contains("timed out")
                    || e.getMessage().toLowerCase(Locale.ROOT).contains("timeout")
                    || e.getMessage().toLowerCase(Locale.ROOT).contains("connect"));
            Assert.assertNotNull(e.getCause());
            Assert.assertFalse(e.getMessage().contains("HttpCode: 0"));
        }

        httpRequest = new HttpRequest(null);
        try {
            CompatibleUrlConnClient.compatibleGetResponse(httpRequest);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("URL is null for HttpRequest.", e.getMessage());
        }

        httpRequest = new HttpRequest("test");
        try {
            CompatibleUrlConnClient.compatibleGetResponse(httpRequest);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("Method is not set for HttpRequest.", e.getMessage());
        }
    }

    @Test
    public void syncInvokeThrowsWhenErrorStreamIsNull() throws Exception {
        CompatibleUrlConnClient client = spy(new CompatibleUrlConnClient());
        HttpRequest request = new HttpRequest("https://sts.aliyuncs.com");
        request.setSysMethod(MethodType.POST);
        request.setSysConnectTimeout(1000);
        request.setSysReadTimeout(1000);

        HttpURLConnection conn = mock(HttpURLConnection.class);
        doReturn(conn).when(client).buildHttpConnection(request);
        doThrow(new ConnectException("Connection refused")).when(conn).connect();
        when(conn.getErrorStream()).thenReturn(null);
        when(conn.getURL()).thenReturn(new URL("https://sts.aliyuncs.com"));

        try {
            client.syncInvoke(request);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Connection refused", e.getMessage());
            Assert.assertTrue(e.getCause() instanceof ConnectException);
            Assert.assertFalse(e.getMessage().contains("HttpCode: 0"));
        }
        verify(conn).disconnect();
    }

    @Test
    public void syncInvokeThrowsWhenErrorStreamNullAndMessageNull() throws Exception {
        CompatibleUrlConnClient client = spy(new CompatibleUrlConnClient());
        HttpRequest request = new HttpRequest("https://sts.aliyuncs.com");
        request.setSysMethod(MethodType.GET);
        request.setSysConnectTimeout(1000);
        request.setSysReadTimeout(1000);

        HttpURLConnection conn = mock(HttpURLConnection.class);
        doReturn(conn).when(client).buildHttpConnection(request);
        IOException noMessage = new IOException();
        doThrow(noMessage).when(conn).connect();
        when(conn.getErrorStream()).thenReturn(null);
        when(conn.getURL()).thenReturn(new URL("https://sts.aliyuncs.com"));

        try {
            client.syncInvoke(request);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("java.io.IOException"));
            Assert.assertSame(noMessage, e.getCause());
        }
        verify(conn).disconnect();
    }

    @Test
    public void syncInvokeParsesHttpErrorWhenErrorStreamPresent() throws Exception {
        CompatibleUrlConnClient client = spy(new CompatibleUrlConnClient());
        HttpRequest request = new HttpRequest("https://sts.aliyuncs.com");
        request.setSysMethod(MethodType.GET);
        request.setSysConnectTimeout(1000);
        request.setSysReadTimeout(1000);

        HttpURLConnection conn = mock(HttpURLConnection.class);
        doReturn(conn).when(client).buildHttpConnection(request);
        doNothing().when(conn).connect();
        when(conn.getInputStream()).thenThrow(new IOException("Server returned HTTP response code: 400"));
        when(conn.getErrorStream()).thenReturn(new ByteArrayInputStream("{\"Code\":\"Invalid\"}".getBytes("UTF-8")));
        when(conn.getResponseCode()).thenReturn(400);
        when(conn.getResponseMessage()).thenReturn("Bad Request");
        when(conn.getURL()).thenReturn(new URL("https://sts.aliyuncs.com"));
        Map<String, java.util.List<String>> headers = new HashMap<String, java.util.List<String>>();
        headers.put("Content-Type", Collections.singletonList("application/json;charset=utf-8"));
        when(conn.getHeaderFields()).thenReturn(headers);

        HttpResponse response = client.syncInvoke(request);
        Assert.assertEquals(400, response.getResponseCode());
        Assert.assertEquals("Bad Request", response.getResponseMessage());
        Assert.assertTrue(response.getHttpContentString().contains("Invalid"));
        Assert.assertTrue(response.toHttpFailureString().contains("HttpCode: 400"));
        Assert.assertTrue(response.toHttpFailureString().contains("ResponseMessage: Bad Request"));
        verify(conn).disconnect();
    }

    @Test
    public void parseHttpConnThrowsWhenContentNull() {
        CompatibleUrlConnClient client = new CompatibleUrlConnClient();
        HttpResponse response = new HttpResponse("https://example.com");
        try {
            client.parseHttpConn(response, mock(HttpURLConnection.class), null,
                    new IOException("Read timed out"));
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Read timed out", e.getMessage());
            Assert.assertTrue(e.getCause() instanceof IOException);
        }
        try {
            client.parseHttpConn(response, mock(HttpURLConnection.class), null, null);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("HTTP request failed without response", e.getMessage());
        }
        try {
            client.parseHttpConn(response, mock(HttpURLConnection.class), null, new IOException());
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("java.io.IOException"));
            Assert.assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Test
    public void buildHttpConnectionTest() {
        CompatibleUrlConnClient client0 = new CompatibleUrlConnClient();
        CompatibleUrlConnClient client = spy(client0);
        HttpRequest request = mock(HttpRequest.class);
        when(request.getSysMethod()).thenReturn(MethodType.POST);
        when(request.getSysUrl()).thenReturn("https://www.aliyun.com");
        when(request.getSysConnectTimeout()).thenReturn(120);
        when(request.getSysReadTimeout()).thenReturn(120);
        Map<String, String> headers = new HashMap<String, String>() {
            {
                put("header1", "value1");
                put("Content-Type", "json");
            }
        };
        when(request.getSysHeaders()).thenReturn(headers);
        when(request.getHeaderValue("header1")).thenReturn("value1");
        when(request.getHeaderValue("Content-Type")).thenReturn("json");
        HttpURLConnection connection = client.buildHttpConnection(request);
        Assert.assertEquals("value1", connection.getRequestProperty("header1"));
        Assert.assertEquals("json", connection.getRequestProperty("Content-Type"));
        Pattern pattern = Pattern.compile("AlibabaCloud (.+; .+) Java/.+ Credentials/.+ TeaDSL/1");
        Matcher matcher = pattern.matcher(connection.getRequestProperty("User-Agent"));
        Assert.assertTrue(matcher.find());
    }
}
