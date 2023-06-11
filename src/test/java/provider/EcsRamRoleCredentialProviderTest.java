package provider;

import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.models.Credential;
import com.aliyun.credentials.provider.ECSMetadataServiceCredentialsFetcher;
import com.aliyun.credentials.provider.EcsRamRoleCredentialProvider;
import com.aliyun.credentials.provider.RefreshResult;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.ParameterHelper;
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
        Credential credential = Credential.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("")
                .type(AuthConstant.ECS_RAM_ROLE)
                .build();
        long expiration = ParameterHelper.getUTCDate("2222-01-28T15:15:56Z").getTime();
        RefreshResult<Credential> refreshResult = RefreshResult.builder(credential)
                .staleTime(expiration)
                .build();
        when(fetcher.fetch(ArgumentMatchers.<CompatibleUrlConnClient>any())).thenReturn(refreshResult);
        provider.setFetcher(fetcher);
        Assert.assertEquals(fetcher, provider.getFetcher());
        Assert.assertEquals(AuthConstant.ECS_RAM_ROLE, provider.getCredentials().getType());
        Assert.assertFalse(provider.isAsyncCredentialUpdateEnabled());
    }
}
