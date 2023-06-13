import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.Credential;
import com.aliyun.credentials.StsCredential;
import com.aliyun.credentials.provider.DefaultCredentialsProvider;
import com.aliyun.credentials.provider.EcsRamRoleCredentialProvider;
import com.aliyun.credentials.provider.RamRoleArnCredentialProvider;
import com.aliyun.credentials.provider.RsaKeyPairCredentialProvider;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CredentialTest {
    @Test
    public void credentialTest() {
        Configuration config = new Configuration();
        config.setType(AuthConstant.ACCESS_KEY);
        config.setAccessKeyId("123456");
        config.setAccessKeySecret("654321");
        Credential credential = new Credential(config);
        Assert.assertEquals("123456", credential.getAccessKeyId());
        Assert.assertEquals("654321", credential.getAccessKeySecret());
        Assert.assertEquals(AuthConstant.ACCESS_KEY, credential.getType());
        Assert.assertNull(credential.getSecurityToken());
    }

    @Test
    public void getProviderTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Configuration config = new Configuration();
        config.setType(AuthConstant.ACCESS_KEY);
        config.setRoleName("test");
        config.setAccessKeySecret("test");
        config.setAccessKeyId("test");
        Credential credential = new Credential(config);
        Class<Credential> clazz = Credential.class;
        Method getProvider = clazz.getDeclaredMethod("getProvider", Configuration.class);
        getProvider.setAccessible(true);
        config.setType(AuthConstant.ECS_RAM_ROLE);
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof EcsRamRoleCredentialProvider);
        config.setType(AuthConstant.RAM_ROLE_ARN);
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof RamRoleArnCredentialProvider);
        config.setType(AuthConstant.RSA_KEY_PAIR);
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof RsaKeyPairCredentialProvider);
        config.setType(null);
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof DefaultCredentialsProvider);
        config.setType("default");
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof DefaultCredentialsProvider);
    }

    @Test
    public void getCredentialTest() {
        Configuration config = new Configuration();
        config.setType(AuthConstant.STS);
        config.setAccessKeyId("test");
        config.setAccessKeySecret("test");
        config.setSecurityToken("test");
        Credential credential = PowerMockito.spy(new Credential(config));
        Assert.assertTrue(credential.getCredential(config) instanceof StsCredential);
        config.setType(AuthConstant.RSA_KEY_PAIR);
        Assert.assertNull(credential.getCredential(config));
    }
}

