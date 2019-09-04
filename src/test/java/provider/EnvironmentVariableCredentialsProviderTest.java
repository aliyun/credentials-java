package provider;

import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.provider.EnvironmentVariableCredentialsProvider;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import org.junit.Assert;
import org.junit.Test;

public class EnvironmentVariableCredentialsProviderTest {
    @Test
    public void getCredentialsTest() throws CredentialException {
        new AuthConstant();
        EnvironmentVariableCredentialsProvider provider = new EnvironmentVariableCredentialsProvider();
        AuthUtils.setClientType("aa");
        Assert.assertNull(provider.getCredentials());

        AuthUtils.setClientType("default");
        AuthUtils.setEnvironmentAccessKeyId("accessKeyIdTest");
        Assert.assertNull(provider.getCredentials());

        AuthUtils.setEnvironmentAccessKeySecret("accessKeyIdTest");
        AlibabaCloudCredentials credential = provider.getCredentials();
        String accessKeyId = credential.getAccessKeyId();
        String accessKeySecret = credential.getAccessKeySecret();
        Assert.assertEquals("accessKeyIdTest", accessKeyId);
        Assert.assertEquals("accessKeyIdTest", accessKeySecret);

        AuthUtils.setEnvironmentAccessKeyId(null);
        Assert.assertNull(provider.getCredentials());

        AuthUtils.setEnvironmentAccessKeyId("");
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (CredentialException e){
            Assert.assertEquals("Environment variable accessKeyId cannot be empty", e.getMessage());
        }
        AuthUtils.setEnvironmentAccessKeyId("a");
        AuthUtils.setEnvironmentAccessKeySecret("");
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (CredentialException e){
            Assert.assertEquals("Environment variable accessKeySecret cannot be empty", e.getMessage());
        }

        AuthUtils.setEnvironmentAccessKeyId(null);
        AuthUtils.setEnvironmentAccessKeySecret(null);
    }

}
