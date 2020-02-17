import com.aliyun.credentials.BearerTokenCredential;
import org.junit.Assert;
import org.junit.Test;

public class BearerTokenCredentialsTest {
    @Test
    public void constructorTest() {
        String token = "token";
        BearerTokenCredential credentials = new BearerTokenCredential(token);
        Assert.assertEquals(token, credentials.getBearerToken());
        Assert.assertNull(credentials.getAccessKeyId());
        Assert.assertNull(credentials.getAccessKeySecret());
    }

    @Test
    public void setBearerTokenTest() {
        String token = "token";
        BearerTokenCredential credentials = new BearerTokenCredential(token);
        Assert.assertEquals(token, credentials.getBearerToken());
        String newToken = "new Token";
        credentials.setBearerToken(newToken);
        Assert.assertEquals(newToken, credentials.getBearerToken());
        Assert.assertEquals("bearer", credentials.getType());
        Assert.assertNull(credentials.getSecurityToken());
    }
}
