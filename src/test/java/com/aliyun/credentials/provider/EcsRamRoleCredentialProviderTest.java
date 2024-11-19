package com.aliyun.credentials.provider;

import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.utils.ParameterHelper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EcsRamRoleCredentialProviderTest {

    @Test
    public void constructorTest() {
        try {
            new EcsRamRoleCredentialProvider("");
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Failed to get RAM session credentials from ECS metadata service. HttpCode=0",
                    e.getMessage());
        }

        EcsRamRoleCredentialProvider provider = new EcsRamRoleCredentialProvider("test");
        Assert.assertEquals("ecs_ram_role", provider.getProviderName());
        ECSMetadataServiceCredentialsFetcher fetcher = provider.getFetcher();
        Assert.assertFalse(fetcher.getDisableIMDSv1());
        Assert.assertEquals(1000, fetcher.getReadTimeout());
        Assert.assertEquals(1000, fetcher.getConnectionTimeout());
        Assert.assertEquals("test", fetcher.getRoleName());
        Assert.assertEquals("http://100.100.100.200/latest/meta-data/ram/security-credentials/test", fetcher.getCredentialUrl().toString());

        Configuration configuration = new Configuration();
        configuration.setRoleName("test");
        configuration.setConnectTimeout(2000);
        configuration.setReadTimeout(2000);
        provider = new EcsRamRoleCredentialProvider(configuration);
        Assert.assertEquals("ecs_ram_role", provider.getProviderName());
        fetcher = provider.getFetcher();
        Assert.assertFalse(fetcher.getDisableIMDSv1());
        Assert.assertEquals(2000, fetcher.getReadTimeout());
        Assert.assertEquals(2000, fetcher.getConnectionTimeout());
        Assert.assertEquals("test", fetcher.getRoleName());
        Assert.assertEquals("http://100.100.100.200/latest/meta-data/ram/security-credentials/test", fetcher.getCredentialUrl().toString());

        configuration.setRoleName(null);
        try {
            new EcsRamRoleCredentialProvider(configuration);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Failed to get RAM session credentials from ECS metadata service. HttpCode=0",
                    e.getMessage());
        }

        Config config = new Config();
        config.roleName = "test";
        config.connectTimeout = 2000;
        config.timeout = 2000;
        provider = new EcsRamRoleCredentialProvider(config);
        Assert.assertEquals("ecs_ram_role", provider.getProviderName());
        fetcher = provider.getFetcher();
        Assert.assertFalse(fetcher.getDisableIMDSv1());
        Assert.assertEquals(2000, fetcher.getReadTimeout());
        Assert.assertEquals(2000, fetcher.getConnectionTimeout());
        Assert.assertEquals("test", fetcher.getRoleName());
        Assert.assertEquals("http://100.100.100.200/latest/meta-data/ram/security-credentials/test", fetcher.getCredentialUrl().toString());

        config.disableIMDSv1 = true;
        provider = new EcsRamRoleCredentialProvider(config);
        Assert.assertEquals("ecs_ram_role", provider.getProviderName());
        fetcher = provider.getFetcher();
        Assert.assertTrue(fetcher.getDisableIMDSv1());
        Assert.assertEquals(2000, fetcher.getReadTimeout());
        Assert.assertEquals(2000, fetcher.getConnectionTimeout());
        Assert.assertEquals("test", fetcher.getRoleName());
        Assert.assertEquals("http://100.100.100.200/latest/meta-data/ram/security-credentials/test", fetcher.getCredentialUrl().toString());

        config.roleName = null;
        try {
            new EcsRamRoleCredentialProvider(config);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Failed to get token from ECS Metadata Service, and fallback to IMDS v1 is disabled via the disableIMDSv1 configuration is turned on. Original error: Failed to get token from ECS Metadata Service. HttpCode=0, ResponseMessage=",
                    e.getMessage());
        }

        config.disableIMDSv1 = false;
        try {
            new EcsRamRoleCredentialProvider(config);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Failed to get RAM session credentials from ECS metadata service. HttpCode=0",
                    e.getMessage());
        }

        AuthUtils.disableECSMetaData(true);
        try {
            EcsRamRoleCredentialProvider.builder()
                    .roleName("test")
                    .readTimeout(2000)
                    .connectionTimeout(2000)
                    .disableIMDSv1(false)
                    .enableIMDSv2(true)
                    .metadataTokenDuration(1000)
                    .build();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("IMDS credentials is disabled.",
                    e.getMessage());
        }
        AuthUtils.disableECSMetaData(false);
        provider.close();
    }

    @Test
    public void ecsRamRoleCredentialProviderTest() {
        EcsRamRoleCredentialProvider provider = new EcsRamRoleCredentialProvider("test");
        ECSMetadataServiceCredentialsFetcher fetcher = mock(ECSMetadataServiceCredentialsFetcher.class);
        CredentialModel credential = CredentialModel.builder()
                .accessKeyId("test")
                .accessKeySecret("test")
                .securityToken("")
                .type(AuthConstant.ECS_RAM_ROLE)
                .build();
        long expiration = ParameterHelper.getUTCDate("2222-01-28T15:15:56Z").getTime();
        RefreshResult<CredentialModel> refreshResult = RefreshResult.builder(credential)
                .staleTime(expiration)
                .build();
        when(fetcher.fetch(ArgumentMatchers.<CompatibleUrlConnClient>any())).thenReturn(refreshResult);
        provider.setFetcher(fetcher);
        Assert.assertEquals(fetcher, provider.getFetcher());
        Assert.assertEquals(AuthConstant.ECS_RAM_ROLE, provider.getCredentials().getType());
        Assert.assertFalse(provider.isAsyncCredentialUpdateEnabled());
    }
}
