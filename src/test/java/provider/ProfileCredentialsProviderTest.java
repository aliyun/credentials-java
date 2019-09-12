package provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.provider.*;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import org.ini4j.Wini;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;


public class ProfileCredentialsProviderTest {

    @Test
    public void getCredentialsTest() throws ParseException, IOException, CredentialException {
        AuthUtils.setEnvironmentCredentialsFile("");
        ProfileCredentialsProvider provider = new ProfileCredentialsProvider();
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("The specified credentials file is empty", e.getMessage());
        }
        String filePath = ProfileCredentialsProviderTest.class.getClassLoader().
                getResource("configTest.ini").getPath();
        AuthUtils.setEnvironmentCredentialsFile(filePath);
        provider = new ProfileCredentialsProvider();
        Assert.assertNotNull(provider.getCredentials());

        AuthUtils.setClientType("client5");
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Client is not open in the specified credentials file", e.getMessage());
        }

        AuthUtils.setClientType("default");
    }

    @Test
    public void createCredentialTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ProfileCredentialsProvider provider = new ProfileCredentialsProvider();
        Class<ProfileCredentialsProvider> providerClass = ProfileCredentialsProvider.class;
        Method createCredential = providerClass.getDeclaredMethod(
                "createCredential", Map.class, CredentialsProviderFactory.class);
        createCredential.setAccessible(true);
        CredentialsProviderFactory factory = new CredentialsProviderFactory();
        Map<String, String> client = new HashMap<String, String>();
        try {
            createCredential.invoke(provider, client, factory);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("The configured client type is empty", e.getCause().getLocalizedMessage());
        }


        client.put(AuthConstant.INI_TYPE, AuthConstant.INI_TYPE_RAM);
        try {
            createCredential.invoke(provider, client, factory);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("The configured role_name is empty",
                    e.getCause().getLocalizedMessage());
        }
        client.put(AuthConstant.INI_ACCESS_KEY_ID, AuthConstant.INI_TYPE_RAM);

        client.put(AuthConstant.INI_ACCESS_KEY_ID,"test");

        client.clear();
        client.put(AuthConstant.INI_ACCESS_KEY_ID, AuthConstant.INI_TYPE_RAM);
        client.put(AuthConstant.INI_TYPE, "access_key");
        Assert.assertNull(createCredential.invoke(provider, client, factory));
    }

    @Test
    public void getSTSAssumeRoleSessionCredentialsTest() throws NoSuchMethodException {
        ProfileCredentialsProvider provider = new ProfileCredentialsProvider();
        Class<ProfileCredentialsProvider> providerClass = ProfileCredentialsProvider.class;
        Method createCredential = providerClass.getDeclaredMethod(
                "createCredential", Map.class, CredentialsProviderFactory.class);
        createCredential.setAccessible(true);
        CredentialsProviderFactory factory = new CredentialsProviderFactory();
        Map<String, String> client = new HashMap<String, String>();
        client.put(AuthConstant.INI_TYPE, AuthConstant.INI_TYPE_ARN);
        try {
            createCredential.invoke(provider, client, factory);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("The configured access_key_id or access_key_secret is empty",
                    e.getCause().getLocalizedMessage());
        }

        try {
            client.put(AuthConstant.INI_ACCESS_KEY_ID, AuthConstant.INI_TYPE_ARN);
            createCredential.invoke(provider, client, factory);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("The configured access_key_id or access_key_secret is empty",
                    e.getCause().getLocalizedMessage());
        }
        try {
            client.put(AuthConstant.INI_ACCESS_KEY_IDSECRET, AuthConstant.INI_TYPE_ARN);
            createCredential.invoke(provider, client, factory);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("The configured role_session_name or role_arn is empty",
                    e.getCause().getLocalizedMessage());
        }
        try {
            client.put(AuthConstant.INI_ROLE_SESSION_NAME, AuthConstant.INI_TYPE_ARN);
            createCredential.invoke(provider, client, factory);
        } catch (Exception e) {
            Assert.assertEquals("The configured role_session_name or role_arn is empty",
                    e.getCause().getLocalizedMessage());
        }
    }

    @Test
    public void  getSTSGetSessionAccessKeyCredentialsTest() throws NoSuchMethodException{
        ProfileCredentialsProvider provider = new ProfileCredentialsProvider();

        CredentialsProviderFactory factory = new CredentialsProviderFactory();
        Map<String, String> client = new HashMap<String, String>();
        client.put(AuthConstant.INI_TYPE, AuthConstant.INI_TYPE_KEY_PAIR);
        try {
            provider.getSTSGetSessionAccessKeyCredentials(client,factory);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("The configured private_key_file is empty", e.getMessage());
        }
        client.put(AuthConstant.INI_PRIVATE_KEY_FILE, "sads");
        AuthUtils.setPrivateKey("test");
        try {
            provider.getSTSGetSessionAccessKeyCredentials(client,factory);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("The configured public_key_id or private_key_file content is empty",
                    e.getMessage());
        }
        client.put("",AuthConstant.INI_PUBLIC_KEY_ID);
        try {
            provider.getSTSGetSessionAccessKeyCredentials(client,factory);
            Assert.fail();
        }catch (Exception e){
            Assert.assertEquals("The configured public_key_id or private_key_file content is empty",
                    e.getMessage());
        }
        AuthUtils.setPrivateKey(null);
    }


    @Test
    public void createCredentialsProviderTest() throws
            NoSuchMethodException, InvocationTargetException, IllegalAccessException, CredentialException, ParseException {
        ProfileCredentialsProvider profileCredentialsProvider = new ProfileCredentialsProvider();
        Class<ProfileCredentialsProvider> providerClass = ProfileCredentialsProvider.class;
        Method createCredential = providerClass.getDeclaredMethod(
                "createCredential", Map.class, CredentialsProviderFactory.class);
        createCredential.setAccessible(true);
        Map<String, String> client = new HashMap<String, String>();
        client.put(AuthConstant.INI_TYPE, AuthConstant.INI_TYPE_ARN);
        client.put(AuthConstant.INI_ACCESS_KEY_ID, AuthConstant.INI_TYPE_ARN);
        client.put(AuthConstant.INI_ACCESS_KEY_IDSECRET, AuthConstant.INI_TYPE_ARN);
        client.put(AuthConstant.INI_ROLE_SESSION_NAME, AuthConstant.INI_TYPE_ARN);
        client.put(AuthConstant.INI_ROLE_ARN, AuthConstant.INI_TYPE_ARN);
        client.put(AuthConstant.DEFAULT_REGION, AuthConstant.INI_TYPE_ARN);
        RamRoleArnCredentialProvider ramRoleArnCredentialProvider =
                Mockito.mock(RamRoleArnCredentialProvider.class);
        Mockito.when(ramRoleArnCredentialProvider.getCredentials()).thenReturn(null);
        CredentialsProviderFactory factory = Mockito.mock(CredentialsProviderFactory.class);
        Mockito.when(factory.createCredentialsProvider(Mockito.any(RamRoleArnCredentialProvider.class))).
                thenReturn(ramRoleArnCredentialProvider);
        Assert.assertNull(createCredential.invoke(profileCredentialsProvider, client, factory));

        client.clear();
        client.put(AuthConstant.INI_TYPE, AuthConstant.INI_TYPE_KEY_PAIR);
        client.put(AuthConstant.INI_PUBLIC_KEY_ID, AuthConstant.INI_TYPE_KEY_PAIR);
        client.put(AuthConstant.INI_PRIVATE_KEY, AuthConstant.INI_TYPE_KEY_PAIR);
        client.put(AuthConstant.INI_PRIVATE_KEY_FILE, AuthConstant.INI_TYPE_KEY_PAIR);
        AuthUtils.setPrivateKey("test");
        RsaKeyPairCredentialProvider rsaKeyPairCredentialProvider =
                Mockito.mock(RsaKeyPairCredentialProvider.class);
        Mockito.when(rsaKeyPairCredentialProvider.getCredentials()).thenReturn(null);
        Mockito.when(factory.createCredentialsProvider(Mockito.any(RsaKeyPairCredentialProvider.class))).
                thenReturn(rsaKeyPairCredentialProvider);
        Assert.assertNull(createCredential.invoke(profileCredentialsProvider, client, factory));
        AuthUtils.setPrivateKey(null);

        client.clear();
        client.put(AuthConstant.INI_TYPE, AuthConstant.INI_TYPE_RAM);
        client.put(AuthConstant.INI_ROLE_NAME, AuthConstant.INI_TYPE_KEY_PAIR);
        EcsRamRoleCredentialProvider ecsRamRoleCredentialProvider =
                Mockito.mock(EcsRamRoleCredentialProvider.class);
        Mockito.when(ecsRamRoleCredentialProvider.getCredentials()).thenReturn(null);
        Mockito.when(factory.createCredentialsProvider(Mockito.any(EcsRamRoleCredentialProvider.class))).
                thenReturn(ecsRamRoleCredentialProvider);
        Assert.assertNull(createCredential.invoke(profileCredentialsProvider, client, factory));
    }

    @Test
    public void getIniTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ProfileCredentialsProvider profileCredentialsProvider = new ProfileCredentialsProvider();
        Class<ProfileCredentialsProvider> providerClass = ProfileCredentialsProvider.class;
        Method getIni = providerClass.getDeclaredMethod(
                "getIni", String.class);
        getIni.setAccessible(true);
        String file = ProfileCredentialsProviderTest.class.getClassLoader().
                getResource("configTest.ini").getPath();
        Wini firstIni = (Wini) getIni.invoke(profileCredentialsProvider, file);
        Wini secondIni = (Wini) getIni.invoke(profileCredentialsProvider, file);
        Assert.assertTrue(firstIni == secondIni);
    }
}
