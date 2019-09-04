import com.aliyun.credentials.EcsRamRoleCredential;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;

public class EcsRamRoleCredentialTest {
    @Test
    public void constructorTest() throws ParseException {
        EcsRamRoleCredential credential = new EcsRamRoleCredential("id", "secret",
                "token", "2019-08-22T12:12:12Z");

        Assert.assertEquals("id", credential.getAccessKeyId());
        Assert.assertEquals("secret", credential.getAccessKeySecret());
        Assert.assertEquals("token", credential.getSecurityToken());
        Assert.assertEquals(1566475932000L, credential.getExpiration());
    }

    @Test
    public void getSetTest(){
        EcsRamRoleCredential credential = new EcsRamRoleCredential();
        credential.setExpiration(1000L);
        Assert.assertEquals(1000L, credential.getExpiration());
        Assert.assertEquals(AuthConstant.ECS_RAM_ROLE, credential.getType());
    }
}
