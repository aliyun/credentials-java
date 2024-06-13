package http;

import com.aliyun.credentials.http.X509TrustManagerImp;
import org.junit.Test;
import org.mockito.Mockito;

public class X509TrustManagerImpTest {

    @Test
    public void X509TrustManagerTest() {
        X509TrustManagerImp x509TrustManagerImp = Mockito.spy(X509TrustManagerImp.class);
        x509TrustManagerImp.checkClientTrusted(null, null);
        Mockito.verify(x509TrustManagerImp, Mockito.timeout(1)).checkClientTrusted(null,null);
    }
}
