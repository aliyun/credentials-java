import com.aliyun.credentials.RamRoleArnCredential;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.provider.RamRoleArnCredentialProvider;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class RamRoleArnCredentialTest {

    @Test
    public void ramRoleArnCredentialTest() {
        RamRoleArnCredentialProvider provider = Mockito.mock(RamRoleArnCredentialProvider.class);
        CredentialModel credential = CredentialModel.builder()
                .accessKeyId("id")
                .accessKeySecret("secret")
                .securityToken("token")
                .type(AuthConstant.RAM_ROLE_ARN)
                .expiration(64090527132000L)
                .build();
        Mockito.when(provider.getCredentials()).thenReturn(credential);

        RamRoleArnCredential newCredential = new RamRoleArnCredential("idTest", "secretTest",
                "tokenTest", 0L, provider);
        Assert.assertEquals("id", newCredential.getAccessKeyId());
        Assert.assertEquals("secret", newCredential.getAccessKeySecret());
        Assert.assertEquals(64090527132000L, newCredential.getExpiration());
        Assert.assertEquals("token", newCredential.getSecurityToken());
        Assert.assertEquals(AuthConstant.RAM_ROLE_ARN, newCredential.getType());
    }

}
