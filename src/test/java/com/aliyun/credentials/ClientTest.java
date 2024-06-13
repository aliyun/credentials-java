package com.aliyun.credentials;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.provider.*;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.powermock.api.mockito.PowerMockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;

public class ClientTest {

    @Test
    public void credentialTest() throws CredentialException, NoSuchFieldException, IllegalAccessException {
        Config config = new Config();
        config.type = AuthConstant.ACCESS_KEY;
        config.accessKeyId = "123456";
        config.accessKeySecret = "654321";
        Client credential = new Client(config);
        Assert.assertEquals("123456", credential.getAccessKeyId());
        Assert.assertEquals("654321", credential.getAccessKeySecret());
        Assert.assertEquals(AuthConstant.ACCESS_KEY, credential.getType());
        Assert.assertNull(credential.getSecurityToken());

        AuthUtils.setEnvironmentCredentialsFile(null);
        // Clear the contents of the global credentials.ini
        Field field = ProfileCredentialsProvider.class.getDeclaredField("ini");
        field.setAccessible(true);
        field.set(ProfileCredentialsProvider.class, null);
        try {
            credential = new Client();
            credential.getCredential();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("not found credentials", e.getMessage());
        }
        try {
            credential = new Client(new DefaultCredentialsProvider());
            credential.getCredential();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("not found credentials", e.getMessage());
        }
    }

    @Test
    public void getProviderTest() throws ParseException, IOException, CredentialException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Config config = new Config();
        config.type = (AuthConstant.ACCESS_KEY);
        config.roleName = "test";
        config.accessKeySecret = "test";
        config.accessKeyId = "test";
        final Client credential = new Client(config);
        Class<Client> clazz = Client.class;
        final Method getProvider = clazz.getDeclaredMethod("getProvider", Config.class);
        getProvider.setAccessible(true);
        config.type = AuthConstant.ECS_RAM_ROLE;
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof EcsRamRoleCredentialProvider);
        config.type = AuthConstant.RAM_ROLE_ARN;
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof RamRoleArnCredentialProvider);
        config.type = AuthConstant.RSA_KEY_PAIR;
        config.publicKeyId = "test";
        config.privateKeyFile = "/test";
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof RsaKeyPairCredentialProvider);
        config.type = null;
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof DefaultCredentialsProvider);
        config.type = "default";
        Assert.assertThrows("", CredentialException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                getProvider.invoke(credential, config);
            }
        });
    }

    @Test
    public void getCredentialTest() throws Exception {
        Config config = new Config();
        config.type = (AuthConstant.STS);
        config.accessKeyId = "test";
        config.accessKeySecret = "test";
        config.securityToken = "test";
        Client credential = PowerMockito.spy(new Client(config));
        Assert.assertEquals(AuthConstant.STS, credential.getType());
        config.type = AuthConstant.RSA_KEY_PAIR;
        config.publicKeyId = "test";
        config.privateKeyFile = "/test";
        Assert.assertNull(credential.getBearerToken());
    }
}
