import com.aliyun.credentials.AccessKeyCredential;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;

public class AccessKeyCredentialTest {
    @Test
    public void accessKeyCredentialTest(){
        AccessKeyCredential credential;
        try {
            new AccessKeyCredential(null, "test");
            Assert.fail();
        }catch (IllegalArgumentException e){
            Assert.assertEquals("Access key ID cannot be null.",e.getMessage());
        }

        try {
            new AccessKeyCredential("test", null);
            Assert.fail();
        }catch (IllegalArgumentException e){
            Assert.assertEquals("Access key secret cannot be null.",e.getMessage());
        }

        credential = new AccessKeyCredential("test", "test");
        Assert.assertEquals("test", credential.getAccessKeyId());
        Assert.assertEquals("test", credential.getAccessKeySecret());
        Assert.assertEquals(AuthConstant.ACCESS_KEY, credential.getType());
        Assert.assertNull(credential.getSecurityToken());

    }

}
