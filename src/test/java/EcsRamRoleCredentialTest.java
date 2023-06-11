import com.aliyun.credentials.EcsRamRoleCredential;
import com.aliyun.credentials.models.Credential;
import com.aliyun.credentials.provider.EcsRamRoleCredentialProvider;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class EcsRamRoleCredentialTest {
    @Test
    public void constructorTest() {
        EcsRamRoleCredential credential = new EcsRamRoleCredential("id", "secret",
                "token", "2222-08-22T12:12:12Z", null);

        Assert.assertEquals("id", credential.getAccessKeyId());
        Assert.assertEquals("secret", credential.getAccessKeySecret());
        Assert.assertEquals("token", credential.getSecurityToken());
        Assert.assertEquals(7972517532000L, credential.getExpiration());
    }

    @Test
    public void getSetTest() {
        EcsRamRoleCredential credential = new EcsRamRoleCredential();
        credential.setExpiration(1000L);
        Assert.assertEquals(1000L, credential.getExpiration());
        Assert.assertEquals(AuthConstant.ECS_RAM_ROLE, credential.getType());
    }

    @Test
    public void refreshCredentialTest() throws Exception{
        EcsRamRoleCredentialProvider provider = Mockito.mock(EcsRamRoleCredentialProvider.class);

        Credential credential = Credential.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("")
                .type(AuthConstant.ECS_RAM_ROLE)
                .expiration(64090527132000L)
                .build();
        Mockito.when(provider.getCredentials()).thenReturn(credential);

        EcsRamRoleCredential newCredential = new EcsRamRoleCredential("id", "id", "",
                "2019-01-28T15:15:56Z",provider);
        Assert.assertEquals("test", newCredential.getAccessKeyId());
    }
}
