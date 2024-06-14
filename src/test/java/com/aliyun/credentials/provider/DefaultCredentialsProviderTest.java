package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;

public class DefaultCredentialsProviderTest {
    @Test
    public void userConfigurationProvidersTest() {
        SystemPropertiesCredentialsProvider provider = new SystemPropertiesCredentialsProvider();
        DefaultCredentialsProvider.addCredentialsProvider(provider);
        Assert.assertTrue(DefaultCredentialsProvider.containsCredentialsProvider(provider));

        DefaultCredentialsProvider.removeCredentialsProvider(provider);
        Assert.assertFalse(DefaultCredentialsProvider.containsCredentialsProvider(provider));

        DefaultCredentialsProvider.addCredentialsProvider(provider);
        DefaultCredentialsProvider.clearCredentialsProvider();
        Assert.assertFalse(DefaultCredentialsProvider.containsCredentialsProvider(provider));
    }

    @Test
    public void getCredentialsTest() throws NoSuchFieldException, IllegalAccessException {
        DefaultCredentialsProvider provider = new DefaultCredentialsProvider();
        AuthUtils.setEnvironmentECSMetaData("");
        try {
            new DefaultCredentialsProvider();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Failed to get RAM session credentials from ECS metadata service. HttpCode=0",
                    e.getMessage());
        }

        AuthUtils.setEnvironmentAccessKeyId("test");
        AuthUtils.setEnvironmentAccessKeySecret("test");
        CredentialModel credential = provider.getCredentials();
        Assert.assertEquals("test", credential.getAccessKeyId());
        Assert.assertEquals("test", credential.getAccessKeySecret());

        DefaultCredentialsProvider.addCredentialsProvider(new AlibabaCloudCredentialsProvider() {
            @Override
            public CredentialModel getCredentials() {
                throw new CredentialException("test");
            }
        });
        DefaultCredentialsProvider.addCredentialsProvider(new AlibabaCloudCredentialsProvider() {
            @Override
            public CredentialModel getCredentials() {
                return CredentialModel.builder()
                        .accessKeyId("")
                        .accessKeySecret("")
                        .type(AuthConstant.ACCESS_KEY)
                        .build();
            }
        });
        credential = provider.getCredentials();
        Assert.assertEquals("", credential.getAccessKeyId());
        Assert.assertEquals("", credential.getAccessKeySecret());
        Assert.assertEquals(AuthConstant.ACCESS_KEY, credential.getType());

        DefaultCredentialsProvider.clearCredentialsProvider();
        AuthUtils.setEnvironmentECSMetaData(null);
        AuthUtils.setEnvironmentAccessKeyId(null);
        AuthUtils.setEnvironmentAccessKeySecret(null);
        System.setProperty(AuthConstant.SYSTEM_ACCESSKEYID, "");
        System.setProperty(AuthConstant.SYSTEM_ACCESSKEYSECRET, "");
        AuthUtils.setEnvironmentCredentialsFile(null);
        // Clear the contents of the global credentials.ini
        Field field = ProfileCredentialsProvider.class.getDeclaredField("ini");
        field.setAccessible(true);
        field.set(ProfileCredentialsProvider.class, null);
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("Unable to load credentials from any of the providers in the chain"));
        }
    }

    @Test
    public void defaultCredentialsProviderTest() throws ClassCastException, CredentialException, IOException, ParseException {
        DefaultCredentialsProvider.clearCredentialsProvider();
        AuthUtils.setEnvironmentECSMetaData("test");
        AuthUtils.setEnvironmentAccessKeyId("test");
        AuthUtils.setEnvironmentAccessKeySecret("test");
        DefaultCredentialsProvider provider = new DefaultCredentialsProvider();
        Assert.assertEquals("test", provider.getCredentials().getAccessKeyId());
        Assert.assertEquals("test", provider.getCredentials().getAccessKeySecret());
        AuthUtils.setEnvironmentECSMetaData(null);
        AuthUtils.setEnvironmentAccessKeyId(null);
        AuthUtils.setEnvironmentAccessKeySecret(null);
        DefaultCredentialsProvider.clearCredentialsProvider();
    }

    @Test
    public void setClientTypeTest() {
        AuthUtils.setClientType("test");
        Assert.assertEquals("test", AuthUtils.getClientType());
        AuthUtils.setClientType("default");
    }
}
