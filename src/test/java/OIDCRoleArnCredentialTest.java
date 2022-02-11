import com.aliyun.credentials.OIDCRoleArnCredential;
import com.aliyun.credentials.provider.AlibabaCloudCredentialsProvider;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class OIDCRoleArnCredentialTest {

    @Test
    public void OIDCRoleArnCredentialTest() {
        AlibabaCloudCredentialsProvider provider = mock(AlibabaCloudCredentialsProvider.class);
        when(provider.getCredentials()).thenReturn(new OIDCRoleArnCredential("id", "secret",
                "token", 64090527132000L, null));

        OIDCRoleArnCredential credential = new OIDCRoleArnCredential("idTest", "secretTest",
                "tokenTest", 0L, provider);
        credential.refreshCredential();
        Assert.assertEquals("id", credential.getAccessKeyId());
        Assert.assertEquals("secret", credential.getAccessKeySecret());
        Assert.assertEquals(64090527132000L, credential.getExpiration());
        Assert.assertEquals("token", credential.getSecurityToken());
        Assert.assertEquals(AuthConstant.OIDC_ROLE_ARN, credential.getType());
    }

}
