import com.aliyun.credentials.StsCredential;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;

public class StsCredentialTest {
    @Test
    public void constructorTest(){
        StsCredential credential = new StsCredential(
                "id", "secret", "token");
        Assert.assertEquals("id", credential.getAccessKeyId());
        Assert.assertEquals("secret", credential.getAccessKeySecret());
        Assert.assertEquals("token", credential.getSecurityToken());
    }

    @Test
    public void getSetTest(){
        StsCredential credential = new StsCredential();
        credential.setAccessKeyId("id");
        Assert.assertEquals("id", credential.getAccessKeyId());

        credential.setAccessKeySecret("secret");
        Assert.assertEquals("secret", credential.getAccessKeySecret());

        credential.setSecurityToken("token");
        Assert.assertEquals("token", credential.getSecurityToken());
        Assert.assertEquals(AuthConstant.STS, credential.getType());
    }
}
