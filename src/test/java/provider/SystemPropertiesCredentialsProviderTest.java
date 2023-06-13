package provider;

import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.provider.SystemPropertiesCredentialsProvider;
import com.aliyun.credentials.utils.AuthUtils;
import org.junit.Assert;
import org.junit.Test;

public class SystemPropertiesCredentialsProviderTest {

    @Test
    public void getCredentialsTest() {
        SystemPropertiesCredentialsProvider provider = new SystemPropertiesCredentialsProvider();
        AuthUtils.setClientType("aa");
        Assert.assertNull(provider.getCredentials());

        AuthUtils.setClientType("default");
        System.setProperty(AuthConstant.SYSTEM_ACCESSKEYSECRET, "");
        System.setProperty(AuthConstant.SYSTEM_ACCESSKEYID, "accessKeyIdTest");
        Assert.assertNull(provider.getCredentials());

        System.setProperty(AuthConstant.SYSTEM_ACCESSKEYSECRET, "accessKeyIdTest");
        AlibabaCloudCredentials credential = provider.getCredentials();
        String accessKeyId = credential.getAccessKeyId();
        String accessKeySecret = credential.getAccessKeySecret();
        Assert.assertEquals("accessKeyIdTest", accessKeyId);
        Assert.assertEquals("accessKeyIdTest", accessKeySecret);

        System.setProperty(AuthConstant.SYSTEM_ACCESSKEYID, "");
        Assert.assertNull(provider.getCredentials());

        System.setProperty(AuthConstant.SYSTEM_ACCESSKEYSECRET, "");
    }
}
