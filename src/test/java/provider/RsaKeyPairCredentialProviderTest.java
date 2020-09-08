package provider;

import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.RsaKeyPairCredential;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.FormatType;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.provider.RsaKeyPairCredentialProvider;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RsaKeyPairCredentialProviderTest {
    @Test
    public void constructorTest() {
        Configuration config = new Configuration();
        config.setPublicKeyId("test");
        config.setPrivateKeyFile("test");
        config.setConnectTimeout(2000);
        config.setReadTimeout(2000);
        RsaKeyPairCredentialProvider provider = new RsaKeyPairCredentialProvider(config);
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals("test", provider.getPrivateKey());
        Assert.assertEquals("test", provider.getPublicKeyId());
    }

    @Test
    public void getCredentialsTest(){
        RsaKeyPairCredentialProvider provider = new RsaKeyPairCredentialProvider(null, null);
        Assert.assertNull(provider.getCredentials());
    }

    @Test
    public void createCredentialTest() {
        RsaKeyPairCredentialProvider provider = new RsaKeyPairCredentialProvider("test", "test");
        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setHttpContent(new String("{\"SessionAccessKey\":{\"Expiration\":\"2019-12-12T1:1:1Z\",\"SessionAccessKeyId\":\"test\"," +
                "\"SessionAccessKeySecret\":\"test\"}}").getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        Assert.assertTrue(provider.createCredential(client) instanceof RsaKeyPairCredential);
    }

    @Test
    public void getSet(){
        RsaKeyPairCredentialProvider provider = new RsaKeyPairCredentialProvider("test", "test");
        provider.setConnectTimeout(888);
        Assert.assertEquals(888, provider.getConnectTimeout());

        provider.setReadTimeout(999);
        Assert.assertEquals(999, provider.getReadTimeout());

        provider.setRegionId("test");
        Assert.assertEquals("test", provider.getRegionId());

        provider.setDurationSeconds(2000);
        Assert.assertEquals(2000, provider.getDurationSeconds());

        provider.setPrivateKey("test");
        Assert.assertEquals("test", provider.getPrivateKey());

        provider.setPublicKeyId("test");
        Assert.assertEquals("test", provider.getPublicKeyId());
    }
}
