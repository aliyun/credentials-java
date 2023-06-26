package function;

import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.provider.RamRoleArnCredentialProvider;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;

public class HttpClientTest {

    @Test
    public void baseConnect() throws InterruptedException {
        RamRoleArnCredentialProvider provider = new RamRoleArnCredentialProvider(System.getenv("RAMAccessKeyId"),
                System.getenv("RAMAccessKeySecret"), System.getenv("roleArn"));
        provider.setRegionId("cn-hangzhou");
        provider.setExternalId("for-test");
        AlibabaCloudCredentials credentials = provider.getCredentials();
        Assert.assertNotNull(credentials.getSecurityToken());
        Assert.assertNotNull(credentials.getAccessKeyId());
        Assert.assertNotNull(credentials.getAccessKeySecret());

        Config config = new Config();
        config.accessKeyId = System.getenv("RAMAccessKeyId");
        config.accessKeySecret = System.getenv("RAMAccessKeySecret");
        config.roleArn = System.getenv("roleArn");
        config.roleSessionName = "defaultSessionName";
        config.roleSessionExpiration = 3600;
        config.externalId = "for-test";
        config.type = AuthConstant.RAM_ROLE_ARN;
        Client client = new Client(config);
        String ak = client.getAccessKeyId();
        String secret = client.getAccessKeySecret();
        String token = client.getSecurityToken();
        Thread.sleep(10 * 1000);
        Assert.assertEquals(ak, client.getAccessKeyId());
        Assert.assertEquals(secret, client.getAccessKeySecret());
        Assert.assertEquals(token, client.getSecurityToken());

        AlibabaCloudCredentials credential = client.getCredential();
        ak = credential.getAccessKeyId();
        secret = credential.getAccessKeySecret();
        token = credential.getSecurityToken();
        Thread.sleep(10 * 1000);
        credential = client.getCredential();
        Assert.assertEquals(ak, credential.getAccessKeyId());
        Assert.assertEquals(secret, credential.getAccessKeySecret());
        Assert.assertEquals(token, credential.getSecurityToken());
    }
}
