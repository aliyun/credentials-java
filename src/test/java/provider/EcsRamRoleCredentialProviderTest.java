package provider;

import com.aliyun.credentials.EcsRamRoleCredential;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.provider.AlibabaCloudCredentialsProvider;
import com.aliyun.credentials.provider.ECSMetadataServiceCredentialsFetcher;
import com.aliyun.credentials.provider.EcsRamRoleCredentialProvider;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EcsRamRoleCredentialProviderTest {
    @Test
    public void ecsRamRoleCredentialProviderTest() {
        EcsRamRoleCredentialProvider provider = new EcsRamRoleCredentialProvider("test");
        ECSMetadataServiceCredentialsFetcher fetcher = mock(ECSMetadataServiceCredentialsFetcher.class);
        when(fetcher.fetch(ArgumentMatchers.<CompatibleUrlConnClient>any(), ArgumentMatchers.<AlibabaCloudCredentialsProvider>any())).thenReturn(new EcsRamRoleCredential());
        provider.setFetcher(fetcher);
        Assert.assertEquals(fetcher, provider.getFetcher());
        Assert.assertTrue(provider.getCredentials() instanceof  EcsRamRoleCredential);
    }
}
