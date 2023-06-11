import com.aliyun.credentials.URLCredential;
import com.aliyun.credentials.models.Credential;
import com.aliyun.credentials.provider.URLCredentialProvider;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class URLCredentialTest {

    @Test
    public void OIDCRoleArnCredentialTest() {
        URLCredentialProvider provider = Mockito.mock(URLCredentialProvider.class);
        Credential credential = Credential.builder()
                .accessKeyId("id")
                .accessKeySecret("secret")
                .securityToken("token")
                .type(AuthConstant.URL_STS)
                .expiration(64090527132000L)
                .build();
        Mockito.when(provider.getCredentials()).thenReturn(credential);
        URLCredential newCredential = new URLCredential("idTest", "secretTest",
                "tokenTest", 0L, provider);
        Assert.assertEquals("id", newCredential.getAccessKeyId());
        Assert.assertEquals("secret", newCredential.getAccessKeySecret());
        Assert.assertEquals(64090527132000L, newCredential.getExpiration());
        Assert.assertEquals("token", newCredential.getSecurityToken());
        Assert.assertEquals(AuthConstant.URL_STS, newCredential.getType());
    }

}
