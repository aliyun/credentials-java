package http;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.HttpRequest;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpRequestTest {
    @Test
    public void httpRequestTest() {
        HttpRequest request = new HttpRequest("test");
        Assert.assertEquals("test", request.getSysUrl());

        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        map.put("test", "test");
        HttpRequest httpRequest = new HttpRequest("test");
        Assert.assertEquals("test", httpRequest.getSysUrl());

        httpRequest = new HttpRequest("test");
        Assert.assertEquals("test", httpRequest.getSysUrl());
        Assert.assertNotNull(request.getSysHeaders());
    }

    @Test
    public void setGetTest() throws NoSuchAlgorithmException {
        HttpRequest request = new HttpRequest("test");
        request.setSysUrl("otherTest");
        Assert.assertEquals("otherTest", request.getSysUrl());

        request.setHttpContentType(null);
        Assert.assertNull(request.getHeaderValue("Content-Type"));

        request.setHttpContent(null, null, null);
        Assert.assertNull(request.getHeaderValue("Content-MD5"));
        Assert.assertNull(request.getHeaderValue("Content-Type"));
        Assert.assertEquals("0", request.getHeaderValue("Content-Length"));
        Assert.assertNull(request.getHttpContent());
        Assert.assertNull(request.getHttpContentType());
        Assert.assertNull(request.getSysEncoding());
    }

    @Test
    public void getHttpContentStringTest() throws CredentialException, UnsupportedEncodingException, NoSuchAlgorithmException {
        HttpRequest request = new HttpRequest("test");
        request.setHttpContent("test".getBytes(), null, null);
        String content = request.getHttpContentString();
        Assert.assertEquals("test", content);

        request = new HttpRequest("test");
        request.setHttpContent("test".getBytes("UTF-8"), "UTF-8", null);
        content = request.getHttpContentString();
        Assert.assertEquals("test", content);

        try {
            request = new HttpRequest("test");
            request.setHttpContent(new byte[]{-1}, "hgbkjhkjh", null);
            request.getHttpContentString();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Can not parse response due to unsupported encoding.", e.getMessage());
        }

    }

    @Test
    public void getUrlTest(){
        HttpRequest request = new HttpRequest();
        request.setUrlParameter("test", "testValue");
        Assert.assertEquals("testValue", request.getUrlParameter("test"));
        Assert.assertTrue(request.getUrlParameters() instanceof Map);
    }

}
