import com.aliyun.credentials.RamRoleArnCredential;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.provider.AlibabaCloudCredentialsProvider;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class RamRoleArnCredentialTest {
    @Test
    public void withShouldRefreshTest() {
        RamRoleArnCredential credential = new RamRoleArnCredential(null, null, null, 0L, null);
        Assert.assertTrue(credential.withShouldRefresh());

        credential = new RamRoleArnCredential(null, null, null, 64090527132000L, null);
        Assert.assertFalse(credential.withShouldRefresh());
    }

    @Test
    public void ramRoleArnCredentialTest() throws CredentialException, IOException, ParseException {
        AlibabaCloudCredentialsProvider provider = mock(AlibabaCloudCredentialsProvider.class);
        when(provider.getCredentials()).thenReturn(new RamRoleArnCredential("id", "secret",
                "token", 64090527132000L, null));

        RamRoleArnCredential credential = new RamRoleArnCredential("idTest", "secretTest",
                "tokenTest", 0L, provider);
        credential.refreshCredential();
        Assert.assertEquals("id", credential.getAccessKeyId());
        Assert.assertEquals("secret", credential.getAccessKeySecret());
        Assert.assertEquals(64090527132000L, credential.getExpiration());
        Assert.assertEquals("token", credential.getSecurityToken());
        Assert.assertEquals(AuthConstant.RAM_ROLE_ARN, credential.getType());
    }

    @Test
    public void getNewRamRoleArnCredentialTest(){
        RamRoleArnCredential credential = new RamRoleArnCredential(null, null, null, 0, null);
        Assert.assertNull(credential.getNewRamRoleArnCredential());
    }
}
