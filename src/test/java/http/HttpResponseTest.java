package http;

import com.aliyun.credentials.http.HttpResponse;
import org.junit.Assert;
import org.junit.Test;

public class HttpResponseTest {
    private HttpResponse response;

    @Test
    public void httpResponseTest() {
        response = new HttpResponse("test");
        Assert.assertEquals("test", response.getSysUrl());
    }
    
    @Test
    public void getSetTest() {
        response = new HttpResponse("test");
        response.setResponseCode(200);
        Assert.assertEquals(200, response.getResponseCode());
        response.setResponseMessage("OK");
        Assert.assertEquals("OK", response.getResponseMessage());
    }
}
