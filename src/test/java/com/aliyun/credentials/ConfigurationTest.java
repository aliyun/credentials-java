package com.aliyun.credentials;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;

public class ConfigurationTest {
    @Test
    public void getSetTest() {
        Configuration config = new Configuration();
        config.setSecurityToken("1");
        Assert.assertEquals("1", config.getSecurityToken());

        config.setAccessKeySecret("2");
        Assert.assertEquals("2", config.getAccessKeySecret());

        config.setAccessKeyId("3");
        Assert.assertEquals("3", config.getAccessKeyId());

        config.setType(AuthConstant.STS);
        Assert.assertEquals(AuthConstant.STS, config.getType());

        config.setRoleName("4");
        Assert.assertEquals("4", config.getRoleName());

        config.setCertFile("5");
        Assert.assertEquals("5", config.getCertFile());

        config.setCertPassword("6");
        Assert.assertEquals("6", config.getCertPassword());

        config.setHost("7");
        Assert.assertEquals("7", config.getHost());

        config.setPrivateKeyFile("8");
        Assert.assertEquals("8", config.getPrivateKeyFile());

        config.setProxy("9");
        Assert.assertEquals("9", config.getProxy());

        config.setPublicKeyId("10");
        Assert.assertEquals("10", config.getPublicKeyId());

        config.setRoleArn("11");
        Assert.assertEquals("11", config.getRoleArn());

        config.setRoleName("12");
        Assert.assertEquals("12", config.getRoleName());

        config.setConnectTimeout(13);
        Assert.assertEquals(13, config.getConnectTimeout());

        config.setReadTimeout(14);
        Assert.assertEquals(14, config.getReadTimeout());

        config.setRoleSessionName("15");
        Assert.assertEquals("15", config.getRoleSessionName());

        config.setSTSEndpoint("www.aliyun.com");
        Assert.assertEquals("www.aliyun.com", config.getSTSEndpoint());
    }
}
