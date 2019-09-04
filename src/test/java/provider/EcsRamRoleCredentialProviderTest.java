package provider;

import com.aliyun.credentials.EcsRamRoleCredential;
import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.provider.ECSMetadataServiceCredentialsFetcher;
import com.aliyun.credentials.provider.EcsRamRoleCredentialProvider;
import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.text.ParseException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EcsRamRoleCredentialProviderTest {
    @Test
    public void constructorTest() throws MalformedURLException {
        try {
            new EcsRamRoleCredentialProvider("");
            Assert.fail();
        } catch (NullPointerException e) {
            Assert.assertEquals("You must specifiy a valid role name.", e.getMessage());
        }

        try {
            new EcsRamRoleCredentialProvider(new Configuration());
            Assert.fail();
        } catch (NullPointerException e) {
            Assert.assertEquals("You must specifiy a valid role name.", e.getMessage());
        }
    }

    @Test
    public void ecsRamRoleCredentialProviderTest() throws MalformedURLException, CredentialException, ParseException {
        EcsRamRoleCredentialProvider provider = new EcsRamRoleCredentialProvider("test");
        ECSMetadataServiceCredentialsFetcher fetcher = mock(ECSMetadataServiceCredentialsFetcher.class);
        when(fetcher.fetch(any())).thenReturn(new EcsRamRoleCredential());
        provider.setFetcher(fetcher);
        Assert.assertEquals(fetcher, provider.getFetcher());
        Assert.assertTrue(provider.getCredentials() instanceof  EcsRamRoleCredential);
    }
}
