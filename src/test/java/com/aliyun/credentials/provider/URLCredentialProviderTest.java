package com.aliyun.credentials.provider;

import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.FormatType;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.net.MalformedURLException;
import java.net.URL;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class URLCredentialProviderTest {

    @Test
    public void constructorTest() throws MalformedURLException {
        URLCredentialProvider provider;

        provider = URLCredentialProvider.builder()
                .credentialsURI("http://test")
                .build();
        Assert.assertEquals("http://test", provider.getURL());

        provider = URLCredentialProvider.builder()
                .credentialsURI(new URL("http://test"))
                .build();
        Assert.assertEquals("http://test", provider.getURL());
        provider.close();

        try {
            URLCredentialProvider.builder().build();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("Credential URI or environment variable ALIBABA_CLOUD_CREDENTIALS_URI cannot be empty.",
                    e.getMessage());
        }

        provider = URLCredentialProvider.builder()
                .credentialsURI(new URL("http://test1"))
                .credentialsURI("http://test2")
                .readTimeout(2000)
                .connectionTimeout(2000)
                .build();
        Assert.assertEquals("credentials_uri", provider.getProviderName());
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals("http://test2", provider.getURL());
    }

    @Test
    public void getCredentials() {
        URLCredentialProvider provider;
        try {
            URLCredentialProvider.builder()
                    .credentialsURI("url")
                    .build();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("com.aliyun.credentials.exception.CredentialException: Credential URI is not valid.",
                    e.toString());
        }
        provider = URLCredentialProvider.builder()
                .credentialsURI("http://10.10.10.10")
                .connectionTimeout(2000)
                .readTimeout(2000)
                .build();
        Assert.assertEquals(2000, provider.getConnectTimeout());
        Assert.assertEquals(2000, provider.getReadTimeout());
        Assert.assertEquals("http://10.10.10.10", provider.getURL());
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Failed to get credentials from server: http://10.10.10.10"));
        } finally {
            provider.close();
        }

        CompatibleUrlConnClient client = mock(CompatibleUrlConnClient.class);
        HttpResponse response = new HttpResponse("test?test=test");
        response.setResponseCode(200);
        response.setHttpContent(("{\"Code\":\"Success\",  \"AccessKeyId\":\"test\", " +
                        "\"AccessKeySecret\":\"test\", \"SecurityToken\":\"test\",  \"Expiration\":\"2019-08-08T1:1:1Z\"}").getBytes(),
                "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        Assert.assertEquals(AuthConstant.CREDENTIALS_URI, provider.getNewSessionCredentials(client).value().getType());

        response.setHttpContent("test".getBytes(),
                "UTF-8", FormatType.JSON);
        when(client.syncInvoke(ArgumentMatchers.<HttpRequest>any())).thenReturn(response);
        try {
            provider.getNewSessionCredentials(client);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Failed to get credentials from server: http://10.10.10.10"));
        }
    }

    @Test
    public void getSetTest() {
        URLCredentialProvider provider = URLCredentialProvider.builder()
                .credentialsURI("http://10.10.10.10")
                .build();
        Assert.assertEquals("http://10.10.10.10", provider.getURL());
        provider.setConnectTimeout(888);
        Assert.assertEquals(888, provider.getConnectTimeout());
        provider.setReadTimeout(999);
        Assert.assertEquals(999, provider.getReadTimeout());

    }

}