package utils;

import com.aliyun.credentials.utils.AuthUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;


public class AuthUtilsTest {
    @Test
    public void getPrivateKeyTest() {
        new AuthUtils();
        AuthUtils.setPrivateKey(null);
        String path = AuthUtils.class.getClassLoader().getResource("configTest.ini").getPath();
        String privateKey = AuthUtils.getPrivateKey(path);
        Assert.assertNotNull(privateKey);
        Assert.assertEquals(privateKey, AuthUtils.getPrivateKey(path));
    }

    @Test
    public void environmentTest() {
        Assert.assertNull(AuthUtils.getEnvironmentAccessKeyId());
        AuthUtils.setEnvironmentAccessKeyId("test");
        Assert.assertEquals("test", AuthUtils.getEnvironmentAccessKeyId());
        AuthUtils.setEnvironmentAccessKeyId(null);

        Assert.assertNull(AuthUtils.getEnvironmentAccessKeySecret());
        AuthUtils.setEnvironmentAccessKeySecret("test");
        Assert.assertEquals("test", AuthUtils.getEnvironmentAccessKeySecret());
        AuthUtils.setEnvironmentAccessKeySecret(null);

        AuthUtils.setEnvironmentCredentialsFile("test");
        Assert.assertEquals("test", AuthUtils.getEnvironmentCredentialsFile());
        AuthUtils.setEnvironmentCredentialsFile(null);
        Assert.assertNull(AuthUtils.getEnvironmentCredentialsFile());

        Assert.assertNull(AuthUtils.getEnvironmentECSMetaData());
        AuthUtils.setEnvironmentECSMetaData("test");
        Assert.assertEquals("test", AuthUtils.getEnvironmentECSMetaData());
        AuthUtils.setEnvironmentECSMetaData(null);
    }

    @Test
    public void clientTypeTest() {
        AuthUtils.setClientType(null);
        Assert.assertEquals("default", AuthUtils.getClientType());
        AuthUtils.setClientType("test");
        Assert.assertEquals("test", AuthUtils.getClientType());
    }
}
