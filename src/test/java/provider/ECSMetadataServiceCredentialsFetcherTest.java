package provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.FormatType;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.provider.ECSMetadataServiceCredentialsFetcher;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ECSMetadataServiceCredentialsFetcherTest {
    @Test
    public void constructorTest() {
        ECSMetadataServiceCredentialsFetcher fetcher = new ECSMetadataServiceCredentialsFetcher("test");
        Assert.assertEquals("http://100.100.100.200/latest/meta-data/ram/security-credentials/test",
                fetcher.getCredentialUrl().toString());

        fetcher = new ECSMetadataServiceCredentialsFetcher("test", 800, 800);
        Assert.assertEquals("test", fetcher.getRoleName());
        Assert.assertEquals(1000, fetcher.getReadTimeout());
        Assert.assertEquals(1000, fetcher.getConnectionTimeout());
        Assert.assertFalse(fetcher.getEnableIMDSv2());
        Assert.assertEquals(0, fetcher.getMetadataTokenDuration());

        fetcher = new ECSMetadataServiceCredentialsFetcher("id", 1200, 800);
        Assert.assertEquals("id", fetcher.getRoleName());
        Assert.assertEquals(1000, fetcher.getReadTimeout());
        Assert.assertEquals(1200, fetcher.getConnectionTimeout());
        Assert.assertFalse(fetcher.getEnableIMDSv2());
        Assert.assertEquals(0, fetcher.getMetadataTokenDuration());

        fetcher = new ECSMetadataServiceCredentialsFetcher("id", 900, 1200);
        Assert.assertEquals("id", fetcher.getRoleName());
        Assert.assertEquals(1200, fetcher.getReadTimeout());
        Assert.assertEquals(1000, fetcher.getConnectionTimeout());
        Assert.assertFalse(fetcher.getEnableIMDSv2());
        Assert.assertEquals(0, fetcher.getMetadataTokenDuration());

        fetcher = new ECSMetadataServiceCredentialsFetcher("id", true, 180, 900, 1200);
        Assert.assertEquals("id", fetcher.getRoleName());
        Assert.assertEquals(1200, fetcher.getReadTimeout());
        Assert.assertEquals(1000, fetcher.getConnectionTimeout());
        Assert.assertTrue(fetcher.getEnableIMDSv2());
        Assert.assertEquals(180, fetcher.getMetadataTokenDuration());
    }

    @Test
    public void fetchTest() throws CredentialException {
        ECSMetadataServiceCredentialsFetcher fetcher = spy(new ECSMetadataServiceCredentialsFetcher("test"));
        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenThrow(new RuntimeException("test"));
        try {
            fetcher.fetch(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Failed to connect ECS Metadata Service: java.lang.RuntimeException: test",
                    e.getMessage());
        }
        HttpResponse response = new HttpResponse("test");
        response.setResponseCode(500);
        client = mock(CompatibleUrlConnClient.class);
        when(client.syncInvoke(any(HttpRequest.class))).thenReturn(response);
        try {
            fetcher.fetch(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Failed to get RAM session credentials from ECS metadata service. HttpCode=500",
                    e.getMessage());
        }

        response = new HttpResponse("test");
        response.setResponseCode(200);
        response.setHttpContent(new String("{\"Code\":\"Success\",  \"AccessKeyId\":\"test\", " +
                        "\"AccessKeySecret\":\"test\", \"SecurityToken\":\"test\",  \"Expiration\":\"2019-08-08T1:1:1Z\"}").getBytes(),
                "UTF-8", FormatType.JSON);
        client = mock(CompatibleUrlConnClient.class);
        when(client.syncInvoke(any(HttpRequest.class))).thenReturn(response);
        Assert.assertEquals(AuthConstant.ECS_RAM_ROLE, fetcher.fetch(client).value().getType());

        response = new HttpResponse("test");
        response.setResponseCode(200);
        response.setHttpContent(new String("{\"Code\":\"1111\",  \"AccessKeyId\":\"test\", " +
                        "\"AccessKeySecret\":\"test\", \"SecurityToken\":\"test\",  \"Expiration\":\"2019-08-08T1:1:1Z\"}").getBytes(),
                "UTF-8", FormatType.JSON);
        client = mock(CompatibleUrlConnClient.class);
        when(client.syncInvoke(any(HttpRequest.class))).thenReturn(response);
        try {
            fetcher.fetch(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Failed to get RAM session credentials from ECS metadata service.",
                    e.getMessage());
        }

        fetcher = spy(new ECSMetadataServiceCredentialsFetcher("test", true, 180, 900, 1200));
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenThrow(new RuntimeException("test"));
        try {
            fetcher.fetch(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Failed to connect ECS Metadata Service: java.lang.RuntimeException: test",
                    e.getMessage());
        }
        response = new HttpResponse("test");
        response.setResponseCode(500);
        client = mock(CompatibleUrlConnClient.class);
        when(client.syncInvoke(any(HttpRequest.class))).thenReturn(response);
        try {
            fetcher.fetch(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Failed to get token from ECS Metadata Service. HttpCode=500",
                    e.getMessage());
        }

        response = new HttpResponse("test");
        response.setResponseCode(200);
        response.setHttpContent(new String("{\"Code\":\"Success\",  \"AccessKeyId\":\"test\", " +
                        "\"AccessKeySecret\":\"test\", \"SecurityToken\":\"test\",  \"Expiration\":\"2019-08-08T1:1:1Z\"}").getBytes(),
                "UTF-8", FormatType.JSON);
        client = mock(CompatibleUrlConnClient.class);
        when(client.syncInvoke(any(HttpRequest.class))).thenReturn(response);
        Assert.assertEquals(AuthConstant.ECS_RAM_ROLE, fetcher.fetch(client).value().getType());
    }

    @Test
    public void fetchRoleNameTest() throws Exception {
        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = mock(HttpResponse.class);
        when(response.getResponseCode()).thenReturn(404);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        ECSMetadataServiceCredentialsFetcher fetcher = new ECSMetadataServiceCredentialsFetcher("");
        try {
            fetcher.fetchRoleName(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("The role name was not found in the instance", e.getMessage());
        }

        response = mock(HttpResponse.class);
        when(response.getResponseCode()).thenReturn(200);
        when(response.getHttpContent()).thenReturn("roleNameTest".getBytes("UTF-8"));
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        Assert.assertEquals("roleNameTest", fetcher.fetchRoleName(client));
    }
}
