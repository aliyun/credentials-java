package function;

import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.provider.RamRoleArnCredentialProvider;
import org.junit.Assert;
import org.junit.Test;

public class HttpClientTest {

    @Test
    public void baseConnect() {
        RamRoleArnCredentialProvider provider = new RamRoleArnCredentialProvider(System.getenv("AccessKeyId"),
                System.getenv("AccessKeySecret"), System.getenv("RAM_ARN"));
        provider.setRegionId("cn-hangzhou");
        AlibabaCloudCredentials credentials = provider.getCredentials();
        Assert.assertNotNull(credentials.getSecurityToken());
        Assert.assertNotNull(credentials.getAccessKeyId());
        Assert.assertNotNull(credentials.getAccessKeySecret());
    }
}
