package provider;

import com.aliyun.credentials.AccessKeyCredential;
import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.provider.AlibabaCloudCredentialsProvider;
import com.aliyun.credentials.provider.DefaultCredentialsProvider;
import com.aliyun.credentials.provider.SystemPropertiesCredentialsProvider;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
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
    public void getCredentialsTest() {
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
        AlibabaCloudCredentials credential = provider.getCredentials();
        Assert.assertTrue(credential instanceof AccessKeyCredential);

        DefaultCredentialsProvider.addCredentialsProvider(new AlibabaCloudCredentialsProvider() {
            @Override
            public AlibabaCloudCredentials getCredentials() {
                return null;
            }
        });
        DefaultCredentialsProvider.addCredentialsProvider(new AlibabaCloudCredentialsProvider() {
            @Override
            public AlibabaCloudCredentials getCredentials() {
                return new AccessKeyCredential("", "");
            }
        });
        credential = provider.getCredentials();
        Assert.assertTrue(credential instanceof AccessKeyCredential);

        DefaultCredentialsProvider.clearCredentialsProvider();
        AuthUtils.setEnvironmentECSMetaData(null);
        AuthUtils.setEnvironmentAccessKeyId(null);
        AuthUtils.setEnvironmentAccessKeySecret(null);
        System.setProperty(AuthConstant.SYSTEM_ACCESSKEYID, "");
        AuthUtils.setEnvironmentCredentialsFile(null);
        try{
            provider.getCredentials();
            Assert.fail();
        } catch (CredentialException e){
            Assert.assertEquals("not found credentials", e.getMessage());
        }
    }

    @Test
    public void defaultCredentialsProviderTest() throws ClassCastException, CredentialException, IOException, ParseException {
        DefaultCredentialsProvider.clearCredentialsProvider();
        AuthUtils.setEnvironmentECSMetaData("test");
        AuthUtils.setEnvironmentAccessKeyId("test");
        AuthUtils.setEnvironmentAccessKeySecret("test");
        DefaultCredentialsProvider provider = new DefaultCredentialsProvider();
        DefaultCredentialsProvider.addCredentialsProvider(new SystemPropertiesCredentialsProvider());
        Assert.assertTrue(provider.getCredentials() instanceof AccessKeyCredential);
        AuthUtils.setEnvironmentECSMetaData(null);
        AuthUtils.setEnvironmentAccessKeyId(null);
        AuthUtils.setEnvironmentAccessKeySecret(null);
        DefaultCredentialsProvider.clearCredentialsProvider();
    }

    @Test
    public void setClientTypeTest(){
        AuthUtils.setClientType("test");
        Assert.assertEquals("test", AuthUtils.getClientType());
        AuthUtils.setClientType("default");
    }
}
