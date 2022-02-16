package provider;

import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.provider.URLCredentialProvider;
import org.junit.Assert;
import org.junit.Test;

public class URLCredentialProviderTest {

    @Test
    public void constructorTest() {
        URLCredentialProvider provider;
        try {
            provider = new URLCredentialProvider("");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("com.aliyun.credentials.exception.CredentialException: URL cannot be null.",
                    e.toString());
        }
        try {
            provider = new URLCredentialProvider("url");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("com.aliyun.credentials.exception.CredentialException: URL is not valid.",
                    e.toString());
        }
        provider = new URLCredentialProvider("http://test");
        Assert.assertEquals("http://test", provider.getURL());
    }

    @Test
    public void getCredentials() {
        Configuration config = new Configuration();
        config.setAccessKeyId("test");
        config.setAccessKeySecret("test");
        config.setCredentialsURI("url");
        config.setConnectTimeout(2000);
        config.setReadTimeout(2000);
        URLCredentialProvider provider;
        try {
            provider = new URLCredentialProvider("url");
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("com.aliyun.credentials.exception.CredentialException: URL is not valid.",
                    e.toString());
        }
        config.setCredentialsURI("http://10.10.10.10");
        provider = new URLCredentialProvider(config);
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals("http://10.10.10.10", provider.getURL());
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Failed to get credentials from server: http://10.10.10.10"));
        }
    }

    @Test
    public void getSetTest() {
        URLCredentialProvider provider = new URLCredentialProvider("http://10.10.10.10");
        Assert.assertEquals("http://10.10.10.10", provider.getURL());
        provider.setConnectTimeout(888);
        Assert.assertEquals(888, provider.getConnectTimeout());
        provider.setReadTimeout(999);
        Assert.assertEquals(999, provider.getReadTimeout());

    }

}