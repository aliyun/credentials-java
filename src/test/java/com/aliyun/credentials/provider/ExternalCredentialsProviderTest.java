package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.CredentialModel;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ExternalCredentialsProviderTest {

    @Before
    public void skipUnixEchoOnWindows() {
        Assume.assumeFalse(
                "external process tests rely on /bin/echo",
                System.getProperty("os.name").toLowerCase().contains("win")
        );
    }

    @Test
    public void testBuilderValidation() {
        try {
            ExternalCredentialsProvider.builder().build();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("process_command is empty", e.getMessage());
        }
    }

    @Test
    public void testGetCredentialsAK() {
        ExternalCredentialsProvider provider = ExternalCredentialsProvider.builder()
                .processCommand("/bin/echo {\"mode\":\"AK\",\"access_key_id\":\"ak\",\"access_key_secret\":\"sk\"}")
                .build();

        CredentialModel credential = provider.getCredentials();
        Assert.assertEquals("ak", credential.getAccessKeyId());
        Assert.assertEquals("sk", credential.getAccessKeySecret());
        Assert.assertNull(credential.getSecurityToken());
        Assert.assertEquals("external", credential.getProviderName());
    }

    @Test
    public void testGetCredentialsStsTokenWithCallback() {
        final AtomicReference<String> capturedToken = new AtomicReference<>();
        final AtomicReference<Long> capturedExpiration = new AtomicReference<>();

        ExternalCredentialsProvider provider = ExternalCredentialsProvider.builder()
                .processCommand("/bin/echo {\"mode\":\"StsToken\",\"access_key_id\":\"ak\",\"access_key_secret\":\"sk\",\"sts_token\":\"token\",\"expiration\":\"2049-10-20T04:27:09Z\"}")
                .credentialUpdateCallback((accessKeyId, accessKeySecret, securityToken, expiration) -> {
                    capturedToken.set(securityToken);
                    capturedExpiration.set(expiration);
                })
                .build();

        CredentialModel credential = provider.getCredentials();
        Assert.assertEquals("ak", credential.getAccessKeyId());
        Assert.assertEquals("sk", credential.getAccessKeySecret());
        Assert.assertEquals("token", credential.getSecurityToken());
        Assert.assertEquals("token", capturedToken.get());
        Assert.assertTrue(capturedExpiration.get() > 0);
    }

    @Test
    public void testRefreshEveryCallWithoutExpiration() {
        final AtomicInteger callbackCount = new AtomicInteger(0);
        ExternalCredentialsProvider provider = ExternalCredentialsProvider.builder()
                .processCommand("/bin/echo {\"mode\":\"AK\",\"access_key_id\":\"ak\",\"access_key_secret\":\"sk\"}")
                .credentialUpdateCallback((accessKeyId, accessKeySecret, securityToken, expiration) -> callbackCount.incrementAndGet())
                .build();

        provider.getCredentials();
        provider.getCredentials();
        Assert.assertEquals(2, callbackCount.get());
    }

    @Test
    public void testMissingFields() {
        ExternalCredentialsProvider provider = ExternalCredentialsProvider.builder()
                .processCommand("/bin/echo {\"mode\":\"AK\",\"access_key_id\":\"ak\"}")
                .build();

        try {
            provider.getCredentials();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("invalid credential response: access_key_id or access_key_secret is empty", e.getMessage());
        }
    }

    @Test
    public void testMissingStsToken() {
        ExternalCredentialsProvider provider = ExternalCredentialsProvider.builder()
                .processCommand("/bin/echo {\"mode\":\"StsToken\",\"access_key_id\":\"ak\",\"access_key_secret\":\"sk\"}")
                .build();

        try {
            provider.getCredentials();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("invalid StsToken credential response: sts_token is empty", e.getMessage());
        }
    }
}
