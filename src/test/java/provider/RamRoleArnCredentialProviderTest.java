package provider;

import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.RamRoleArnCredential;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.FormatType;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.provider.RamRoleArnCredentialProvider;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RamRoleArnCredentialProviderTest {

    @Test
    public void constructorTest() {
        RamRoleArnCredentialProvider provider = new RamRoleArnCredentialProvider("id", "secret",
                "name", "arn", "region", "policy");
        Assert.assertEquals("name", provider.getRoleSessionName());
        Assert.assertEquals("region", provider.getRegionId());
        Assert.assertEquals("policy", provider.getPolicy());
        Assert.assertEquals("id", provider.getAccessKeyId());
        Assert.assertEquals("secret", provider.getAccessKeySecret());
        Assert.assertEquals("arn", provider.getRoleArn());
    }

    @Test
    public void getCredentials() {
        Configuration config = new Configuration();
        config.setAccessKeyId("test");
        config.setAccessKeySecret("test");
        config.setRoleArn("test");
        config.setConnectTimeout(2000);
        config.setReadTimeout(2000);
        RamRoleArnCredentialProvider provider = new RamRoleArnCredentialProvider(config);
        provider.setPolicy("test");
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals("test", provider.getAccessKeyId());
        Assert.assertEquals("test", provider.getAccessKeySecret());
        Assert.assertEquals("test", provider.getRoleArn());
        Assert.assertNull(provider.getCredentials());
    }

    @Test
    public void createCredentialTest() throws NoSuchAlgorithmException, IOException, KeyManagementException {
        Configuration config = new Configuration();
        RamRoleArnCredentialProvider provider = new RamRoleArnCredentialProvider(config);
        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setHttpContent(new String("{\"Credentials\":{\"Expiration\":\"2019-12-12T1:1:1Z\",\"AccessKeyId\":\"test\"," +
                "\"AccessKeySecret\":\"test\",\"SecurityToken\":\"test\"}}").getBytes(), "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        Assert.assertTrue(provider.createCredential(client) instanceof RamRoleArnCredential);
    }

    @Test
    public void getSetTest() {
        RamRoleArnCredentialProvider provider = new RamRoleArnCredentialProvider(null, null, null);
        provider.setConnectTimeout(888);
        Assert.assertEquals(888, provider.getConnectTimeout());

        provider.setReadTimeout(999);
        Assert.assertEquals(999, provider.getReadTimeout());

        provider.setPolicy("test");
        Assert.assertEquals("test", provider.getPolicy());

        provider.setRegionId("test");
        Assert.assertEquals("test", provider.getRegionId());

        provider.setRoleSessionName("test");
        Assert.assertEquals("test", provider.getRoleSessionName());

        provider.setDurationSeconds(2000);
        Assert.assertEquals(2000, provider.getDurationSeconds());

        provider.setAccessKeyId("test");
        Assert.assertEquals("test", provider.getAccessKeyId());

        provider.setAccessKeySecret("test");
        Assert.assertEquals("test", provider.getAccessKeySecret());
    }

}