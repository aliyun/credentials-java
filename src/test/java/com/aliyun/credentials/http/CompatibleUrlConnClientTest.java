package com.aliyun.credentials.http;

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

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
}
