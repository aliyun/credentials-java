import com.aliyun.credentials.*;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;

public class AccessKeyCredentialTest {
    @Test
    public void accessKeyCredentialTest() {
        AccessKeyCredential credential;
        try {
            new AccessKeyCredential(null, "test");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Access key ID cannot be null.", e.getMessage());
        }

        try {
            new AccessKeyCredential("test", null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Access key secret cannot be null.", e.getMessage());
        }

        credential = new AccessKeyCredential("test", "test");
        Assert.assertEquals("test", credential.getAccessKeyId());
        Assert.assertEquals("test", credential.getAccessKeySecret());
        Assert.assertEquals(AuthConstant.ACCESS_KEY, credential.getType());
        Assert.assertNull(credential.getSecurityToken());

    }

    @Test
    public void getBearerTokenTest() {
        AlibabaCloudCredentials credentials = new AccessKeyCredential("", "");
        Assert.assertNull(credentials.getBearerToken());

        credentials = new EcsRamRoleCredential();
        Assert.assertNull(credentials.getBearerToken());

        credentials = new RamRoleArnCredential(null, null, null, 0, null);
        Assert.assertNull(credentials.getBearerToken());

        credentials = new StsCredential();
        Assert.assertNull(credentials.getBearerToken());

        credentials = new RsaKeyPairCredential("", "", 0, null);
        Assert.assertNull(credentials.getBearerToken());
    }
}
