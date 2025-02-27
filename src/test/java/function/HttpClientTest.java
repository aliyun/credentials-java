package function;

import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.provider.RamRoleArnCredentialProvider;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;

public class HttpClientTest {

    @Test
    public void baseConnect() throws InterruptedException {
        RamRoleArnCredentialProvider provider = RamRoleArnCredentialProvider.builder()
                .accessKeyId(System.getenv("RAMAccessKeyId"))
                .accessKeySecret(System.getenv("RAMAccessKeySecret"))
                .roleArn(System.getenv("roleArn"))
                .externalId("for-test")
                .build();
        CredentialModel credentials = provider.getCredentials();
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
        credentials = client.getCredential();
        String ak = credentials.getAccessKeyId();
        String secret = credentials.getAccessKeySecret();
        String token = credentials.getSecurityToken();
        Thread.sleep(10 * 1000);
        Assert.assertEquals(ak, credentials.getAccessKeyId());
        Assert.assertEquals(secret, credentials.getAccessKeySecret());
        Assert.assertEquals(token, credentials.getSecurityToken());

        credentials = client.getCredential();
        ak = credentials.getAccessKeyId();
        secret = credentials.getAccessKeySecret();
        token = credentials.getSecurityToken();
        Thread.sleep(10 * 1000);
        credentials = client.getCredential();
        Assert.assertEquals(ak, credentials.getAccessKeyId());
        Assert.assertEquals(secret, credentials.getAccessKeySecret());
        Assert.assertEquals(token, credentials.getSecurityToken());
    }
}
