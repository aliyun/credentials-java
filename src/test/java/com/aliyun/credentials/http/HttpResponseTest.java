package com.aliyun.credentials.http;

import org.junit.Assert;
import org.junit.Test;

public class HttpResponseTest {

    @Test
    public void toHttpFailureStringIncludesCodeMessageAndBody() {
        HttpResponse response = new HttpResponse("https://example.com");
        response.setResponseCode(403);
        response.setResponseMessage("Forbidden");
        response.setHttpContent("{\"Message\":\"denied\"}".getBytes(), "UTF-8", FormatType.JSON);

        String detail = response.toHttpFailureString();
        Assert.assertEquals("HttpCode: 403, ResponseMessage: Forbidden, result: {\"Message\":\"denied\"}", detail);
    }

    @Test
    public void toHttpFailureStringOmitsEmptyResponseMessage() {
        HttpResponse response = new HttpResponse("https://example.com");
        response.setResponseCode(500);
        response.setResponseMessage("");
        Assert.assertEquals("HttpCode: 500, result: ", response.toHttpFailureString());

        response.setResponseMessage(null);
        Assert.assertEquals("HttpCode: 500, result: ", response.toHttpFailureString());
    }
}
