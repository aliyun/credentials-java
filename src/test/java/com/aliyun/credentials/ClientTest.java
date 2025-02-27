package com.aliyun.credentials;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.provider.*;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ClientTest {

    @Test
    public void credentialTest() throws CredentialException, NoSuchFieldException, IllegalAccessException {
        Config config = new Config();
        config.type = AuthConstant.ACCESS_KEY;
        config.accessKeyId = "123456";
        config.accessKeySecret = "654321";
        Client credential = new Client(config);
        CredentialModel model = credential.getCredential();
        Assert.assertEquals("123456", model.getAccessKeyId());
        Assert.assertEquals("654321", model.getAccessKeySecret());
        Assert.assertEquals(AuthConstant.ACCESS_KEY, model.getType());
        Assert.assertNull(model.getSecurityToken());
    }

    @Test
    public void defaultCredentialTest() {
        AuthUtils.disableCLIProfile(true);
        AuthUtils.setEnvironmentCredentialsFile("");
        try {
            Client credential = new Client();
            credential.getCredential();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Unable to load credentials from any of the providers in the chain"));
        }
        try {
            Client credential = new Client(new DefaultCredentialsProvider());
            credential.getCredential();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Unable to load credentials from any of the providers in the chain"));
        }
        AuthUtils.disableCLIProfile(false);
    }

    @Test
    public void getProviderTest() throws CredentialException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Config config = new Config();
        config.type = AuthConstant.ACCESS_KEY;
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
        config.roleArn = "arn:aws:iam::123456789012:role/test";
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof RamRoleArnCredentialProvider);
        config.type = AuthConstant.RSA_KEY_PAIR;
        config.publicKeyId = "test";
        String file = ProfileCredentialsProviderTest.class.getClassLoader().
                getResource("private_key.txt").getPath();
        config.privateKeyFile = file;
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof RsaKeyPairCredentialProvider);
        config.type = "default";
        try {
            getProvider.invoke(credential, config);
            Assert.fail();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertEquals("invalid type option, support: access_key, sts, ecs_ram_role, ram_role_arn, rsa_key_pair", e.getCause().getLocalizedMessage());
        }
        config.type = AuthConstant.RAM_ROLE_ARN;
        RamRoleArnCredentialProvider provider = (RamRoleArnCredentialProvider) getProvider.invoke(credential, config);
        Class<RamRoleArnCredentialProvider> ramClazz = RamRoleArnCredentialProvider.class;
        Method getCredentialsProvider = ramClazz.getDeclaredMethod("getCredentialsProvider");
        getCredentialsProvider.setAccessible(true);
        Assert.assertTrue(getCredentialsProvider.invoke(provider) instanceof StaticCredentialsProvider);
        Assert.assertEquals(AuthConstant.ACCESS_KEY, ((StaticCredentialsProvider) getCredentialsProvider.invoke(provider)).getCredentials().getType());
        config.securityToken = "";
        provider = (RamRoleArnCredentialProvider) getProvider.invoke(credential, config);
        Assert.assertTrue(getCredentialsProvider.invoke(provider) instanceof StaticCredentialsProvider);
        Assert.assertEquals(AuthConstant.ACCESS_KEY, ((StaticCredentialsProvider) getCredentialsProvider.invoke(provider)).getCredentials().getType());
        Assert.assertNull(((StaticCredentialsProvider) getCredentialsProvider.invoke(provider)).getCredentials().getSecurityToken());
        config.securityToken = "token";
        provider = (RamRoleArnCredentialProvider) getProvider.invoke(credential, config);
        Assert.assertTrue(getCredentialsProvider.invoke(provider) instanceof StaticCredentialsProvider);
        Assert.assertEquals(AuthConstant.STS, ((StaticCredentialsProvider) getCredentialsProvider.invoke(provider)).getCredentials().getType());
        Assert.assertEquals("token", ((StaticCredentialsProvider) getCredentialsProvider.invoke(provider)).getCredentials().getSecurityToken());

        config.type = AuthConstant.BEARER;
        config.bearerToken = "token";
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof StaticCredentialsProvider);

        config.type = AuthConstant.OIDC_ROLE_ARN;
        config.roleArn = "test";
        config.roleSessionExpiration = 3600;
        config.oidcProviderArn = "test";
        config.oidcTokenFilePath = "test";
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof OIDCRoleArnCredentialProvider);

        config.type = AuthConstant.CREDENTIALS_URI;
        config.credentialsURI = "http://test";
        Assert.assertTrue(getProvider.invoke(credential, config) instanceof URLCredentialProvider);
    }

    @Test
    public void getCredentialTest() throws Exception {
        Config config = new Config();
        config.type = (AuthConstant.STS);
        config.accessKeyId = "test";
        config.accessKeySecret = "test";
        config.securityToken = "test";
        Client credential = new Client(config);
        CredentialModel model = credential.getCredential();
        Assert.assertEquals(AuthConstant.STS, model.getType());
        config.type = AuthConstant.RSA_KEY_PAIR;
        config.publicKeyId = "test";
        config.privateKeyFile = "/test";
        Assert.assertNull(model.getBearerToken());
    }
}
