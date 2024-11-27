package com.aliyun.credentials.http;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.*;

import java.net.HttpURLConnection;
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
        response = CompatibleUrlConnClient.compatibleGetResponse(httpRequest);
        Assert.assertEquals("connect timed out", response.getResponseMessage().toLowerCase(Locale.ROOT));

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
