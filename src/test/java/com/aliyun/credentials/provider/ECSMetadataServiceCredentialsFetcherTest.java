package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.FormatType;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.http.MethodType;
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
        Assert.assertEquals(800, fetcher.getReadTimeout());
        Assert.assertEquals(800, fetcher.getConnectionTimeout());
        Assert.assertFalse(fetcher.getDisableIMDSv1());
        Assert.assertEquals(21600, fetcher.getMetadataTokenDuration());

        fetcher = new ECSMetadataServiceCredentialsFetcher("id", 1200, 800);
        Assert.assertEquals("id", fetcher.getRoleName());
        Assert.assertEquals(800, fetcher.getReadTimeout());
        Assert.assertEquals(1200, fetcher.getConnectionTimeout());
        Assert.assertFalse(fetcher.getDisableIMDSv1());
        Assert.assertEquals(21600, fetcher.getMetadataTokenDuration());

        fetcher = new ECSMetadataServiceCredentialsFetcher("id", 900, 1200);
        Assert.assertEquals("id", fetcher.getRoleName());
        Assert.assertEquals(1200, fetcher.getReadTimeout());
        Assert.assertEquals(900, fetcher.getConnectionTimeout());
        Assert.assertFalse(fetcher.getDisableIMDSv1());
        Assert.assertEquals(21600, fetcher.getMetadataTokenDuration());

        fetcher = new ECSMetadataServiceCredentialsFetcher("id", true, 180, 900, 1200);
        Assert.assertEquals("id", fetcher.getRoleName());
        Assert.assertEquals(1200, fetcher.getReadTimeout());
        Assert.assertEquals(900, fetcher.getConnectionTimeout());
        Assert.assertTrue(fetcher.getDisableIMDSv1());
        Assert.assertEquals(21600, fetcher.getMetadataTokenDuration());

        fetcher = new ECSMetadataServiceCredentialsFetcher("id", true, 900, 1200);
        Assert.assertEquals("id", fetcher.getRoleName());
        Assert.assertEquals(1200, fetcher.getReadTimeout());
        Assert.assertEquals(900, fetcher.getConnectionTimeout());
        Assert.assertTrue(fetcher.getDisableIMDSv1());
        Assert.assertEquals(21600, fetcher.getMetadataTokenDuration());
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

        fetcher = spy(new ECSMetadataServiceCredentialsFetcher("test", false, 180, 900, 1200));
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenThrow(new RuntimeException("test"));
        try {
            fetcher.fetch(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Failed to connect ECS Metadata Service: java.lang.RuntimeException: test",
                    e.getMessage());
        }

        fetcher = spy(new ECSMetadataServiceCredentialsFetcher("test", true, 180, 900, 1200));
        try {
            fetcher.fetch(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Failed to get token from ECS Metadata Service, and fallback to IMDS v1 is disabled via the disableIMDSv1 configuration is turned on. Original error: Failed to connect ECS Metadata Service: java.lang.RuntimeException: test",
                    e.getMessage());
        }

        response = new HttpResponse("test");
        response.setHttpContent("no token".getBytes(), "UTF-8", FormatType.PLAIN);
        response.setResponseCode(500);
        client = mock(CompatibleUrlConnClient.class);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        try {
            fetcher.fetch(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Failed to get token from ECS Metadata Service, and fallback to IMDS v1 is disabled via the disableIMDSv1 configuration is turned on. Original error: Failed to get token from ECS Metadata Service. HttpCode=500, ResponseMessage=no token",
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
            Assert.assertEquals("The role name was not found in the instance.", e.getMessage());
        }

        response = mock(HttpResponse.class);
        when(response.getResponseCode()).thenReturn(200);
        when(response.getHttpContent()).thenReturn("roleNameTest".getBytes("UTF-8"));
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        Assert.assertEquals("roleNameTest", fetcher.fetchRoleName(client));
    }

    @Test
    public void fallbackToIMDSv1WhenGetFailsAfterTokenOk() throws Exception {
        ECSMetadataServiceCredentialsFetcher fetcher = new ECSMetadataServiceCredentialsFetcher("test");
        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        when(client.syncInvoke(any(HttpRequest.class))).thenAnswer(invocation -> {
            HttpRequest req = invocation.getArgument(0);
            if (MethodType.PUT.equals(req.getSysMethod())) {
                HttpResponse token = new HttpResponse("token");
                token.setResponseCode(200);
                token.setHttpContent("tokenxxxxx".getBytes(), "UTF-8", FormatType.PLAIN);
                return token;
            }
            if (req.getHeaderValue("X-aliyun-ecs-metadata-token") != null) {
                HttpResponse fail = new HttpResponse("fail");
                fail.setResponseCode(500);
                fail.setHttpContent("v2 failed".getBytes(), "UTF-8", FormatType.PLAIN);
                return fail;
            }
            HttpResponse ok = new HttpResponse("ok");
            ok.setResponseCode(200);
            ok.setHttpContent(("{\"Code\":\"Success\",  \"AccessKeyId\":\"akid\", " +
                    "\"AccessKeySecret\":\"aksecret\", \"SecurityToken\":\"ststoken\",  \"Expiration\":\"2200-08-08T01:01:01Z\"}").getBytes(),
                    "UTF-8", FormatType.JSON);
            return ok;
        });
        Assert.assertEquals("akid", fetcher.fetch(client).value().getAccessKeyId());
        Assert.assertEquals("aksecret", fetcher.fetch(client).value().getAccessKeySecret());
    }

    @Test
    public void noFallbackWhenDisableIMDSv1() throws Exception {
        ECSMetadataServiceCredentialsFetcher fetcher = new ECSMetadataServiceCredentialsFetcher("test", true, 1000, 1000);
        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        when(client.syncInvoke(any(HttpRequest.class))).thenAnswer(invocation -> {
            HttpRequest req = invocation.getArgument(0);
            if (MethodType.PUT.equals(req.getSysMethod())) {
                HttpResponse token = new HttpResponse("token");
                token.setResponseCode(200);
                token.setHttpContent("tokenxxxxx".getBytes(), "UTF-8", FormatType.PLAIN);
                return token;
            }
            HttpResponse fail = new HttpResponse("fail");
            fail.setResponseCode(500);
            return fail;
        });
        try {
            fetcher.fetch(client);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("Failed to get RAM session credentials from ECS metadata service. HttpCode=500",
                    e.getMessage());
        }
    }

}
