package com.aliyun.credentials.http;

import com.aliyun.credentials.exception.CredentialException;
import org.junit.Assert;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class HttpMessageTest {

    @Test
    public void deprecatedSetGetTest() {
        HttpRequest request = new HttpRequest();
        request.setSysMethod(MethodType.PUT);
        Assert.assertTrue(MethodType.PUT == request.getSysMethod());
        Assert.assertTrue(FormatType.JSON == request.getHttpContentType());

        request.setSysEncoding("encodingTest");
        Assert.assertEquals("encodingTest", request.getSysEncoding());

        request.setSysConnectTimeout(88);
        Assert.assertEquals(88, (int) request.getSysConnectTimeout());

        request.setSysReadTimeout(66);
        Assert.assertEquals(66, (int) request.getSysReadTimeout());

        Assert.assertTrue(request.getSysHeaders() instanceof Map);
    }

    @Test
    public void headerParameterTest() {
        HttpRequest request = new HttpRequest("test");
        request.putHeaderParameter(null, null);
        Assert.assertNull(request.getHeaderValue(null));

        request.putHeaderParameter(null, "test");
        Assert.assertNull(request.getHeaderValue(null));

        request.putHeaderParameter("valueNulTest", null);
        Assert.assertNull(request.getHeaderValue("valueNulTest"));

        request.putHeaderParameter("test", "test");
        Assert.assertEquals("test", "test");
    }

    @Test
    public void setGetTest() {
        HttpRequest request = new HttpRequest("test");
        request.setSysMethod(MethodType.PUT);
        Assert.assertTrue(MethodType.PUT == request.getSysMethod());
        Assert.assertTrue(FormatType.JSON == request.getHttpContentType());

        request.setSysEncoding("encodingTest");
        Assert.assertEquals("encodingTest", request.getSysEncoding());

        request.setSysConnectTimeout(88);
        Assert.assertEquals(88, (int) request.getSysConnectTimeout());

        request.setSysReadTimeout(66);
        Assert.assertEquals(66, (int) request.getSysReadTimeout());

        request.setHttpContent(null, null, null);
        Assert.assertEquals("", request.getHttpContentString());
    }

    @Test
    public void setHttpContentWillNullTest() throws CredentialException, NoSuchAlgorithmException {
        HttpRequest request = new HttpRequest("test");
        request.setSysMethod(MethodType.PUT);

        request.setHttpContent(null, null, null);
        Assert.assertEquals(null, request.getHeaderValue("Content-MD5"));
    }

    @Test
    public void setHttpContentWillGETTest() throws CredentialException, NoSuchAlgorithmException {
        HttpRequest request = new HttpRequest("test");
        request.setSysMethod(MethodType.GET);

        request.setHttpContent("content".getBytes(), null, null);
        // md5 of empty string
        Assert.assertEquals("1B2M2Y8AsgTpgAmY7PhCfg==", request.getHeaderValue("Content-MD5"));
    }

    @Test
    public void setHttpContentWillPOSTTest() throws CredentialException, NoSuchAlgorithmException {
        HttpRequest request = new HttpRequest("test");
        request.setSysMethod(MethodType.POST);

        request.setHttpContent("content".getBytes(), null, FormatType.XML);
        // md5 of "content"
        Assert.assertEquals("mgNkuembtIDdJeHwKEyFVQ==", request.getHeaderValue("Content-MD5"));
    }
}
