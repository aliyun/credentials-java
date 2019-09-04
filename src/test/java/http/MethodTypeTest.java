package http;

import com.aliyun.credentials.http.MethodType;
import org.junit.Assert;
import org.junit.Test;

public class MethodTypeTest {

    @Test
    public void testMethodType() {
        Assert.assertFalse("GET has content", MethodType.GET.hasContent());
    }
}