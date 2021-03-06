import com.aliyun.credentials.Client;
import com.aliyun.credentials.StsCredential;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.provider.DefaultCredentialsProvider;
import com.aliyun.credentials.provider.EcsRamRoleCredentialProvider;
import com.aliyun.credentials.provider.RamRoleArnCredentialProvider;
import com.aliyun.credentials.provider.RsaKeyPairCredentialProvider;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;

public class ClientTest {

    @Test
    public void credentialTest() throws ParseException, IOException, CredentialException {
        Config config = new Config();
        config.type = AuthConstant.ACCESS_KEY;
        config.accessKeyId = "123456";
        config.accessKeySecret = "654321";
        Client credential = new Client(config);
        Assert.assertEquals("123456", credential.getAccessKeyId());
        Assert.assertEquals("654321", credential.getAccessKeySecret());
        Assert.assertEquals(AuthConstant.ACCESS_KEY, credential.getType());
        Assert.assertNull(credential.getSecurityToken());
    }

    @Test
    public void getProviderTest() throws ParseException, IOException, CredentialException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Config config = new Config();
        config.type = (AuthConstant.ACCESS_KEY);
        config.roleName = "test";
        config.accessKeySecret = "test";
        config.accessKeyId = "test";
        Client credential = new Client(config);
        Class<Client> clazz = Client.class;
        Method getProvider = clazz.getDeclaredMethod("getProvider", Config.class);
        getProvider.setAccessible(true);
        config.type = AuthConstant.ECS_RAM_ROLE;
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof EcsRamRoleCredentialProvider);
        config.type = AuthConstant.RAM_ROLE_ARN;
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof RamRoleArnCredentialProvider);
        config.type = AuthConstant.RSA_KEY_PAIR;
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof RsaKeyPairCredentialProvider);
        config.type = null;
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof DefaultCredentialsProvider);
        config.type = "default";
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof DefaultCredentialsProvider);
    }

    @Test
    public void getCredentialTest() throws Exception {
        Config config = new Config();
        config.type = (AuthConstant.STS);
        config.accessKeyId = "test";
        config.accessKeySecret = "test";
        config.securityToken = "test";
        Client credential = PowerMockito.spy(new Client(config));
        Assert.assertTrue(credential.getCredential(config) instanceof StsCredential);
        config.type = AuthConstant.RSA_KEY_PAIR;
        Assert.assertNull(credential.getCredential(config));
        Assert.assertNull(credential.getBearerToken());
    }
}
