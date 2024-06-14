import com.aliyun.credentials.RamRoleArnCredential;
import com.aliyun.credentials.provider.AlibabaCloudCredentialsProvider;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class RamRoleArnCredentialTest {

    @Test
    public void ramRoleArnCredentialTest() {
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

}
