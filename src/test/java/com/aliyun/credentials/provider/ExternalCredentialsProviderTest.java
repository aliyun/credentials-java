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
                .processCommand("/bin/echo '{\"mode\":\"AK\",\"access_key_id\":\"ak\",\"access_key_secret\":\"sk\"}'")
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
                .processCommand("/bin/echo '{\"mode\":\"StsToken\",\"access_key_id\":\"ak\",\"access_key_secret\":\"sk\",\"sts_token\":\"token\",\"expiration\":\"2049-10-20T04:27:09Z\"}'")
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
                .processCommand("/bin/echo '{\"mode\":\"AK\",\"access_key_id\":\"ak\",\"access_key_secret\":\"sk\"}'")
                .credentialUpdateCallback((accessKeyId, accessKeySecret, securityToken, expiration) -> callbackCount.incrementAndGet())
                .build();

        provider.getCredentials();
        provider.getCredentials();
        Assert.assertEquals(2, callbackCount.get());
    }

    @Test
    public void testMissingFields() {
        ExternalCredentialsProvider provider = ExternalCredentialsProvider.builder()
                .processCommand("/bin/echo '{\"mode\":\"AK\",\"access_key_id\":\"ak\"}'")
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
                .processCommand("/bin/echo '{\"mode\":\"StsToken\",\"access_key_id\":\"ak\",\"access_key_secret\":\"sk\"}'")
                .build();

        try {
            provider.getCredentials();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertEquals("invalid StsToken credential response: sts_token is empty", e.getMessage());
        }
    }

    @Test
    public void testCommandFailureTimeoutAndInvalidOutput() {
        ExternalCredentialsProvider failed = ExternalCredentialsProvider.builder()
                .processCommand("/bin/sh -c 'echo boom >&2; exit 3'")
                .build();
        try {
            failed.getCredentials();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("exit status 3"));
            Assert.assertTrue(e.getMessage().contains("boom"));
        }

        ExternalCredentialsProvider timedOut = ExternalCredentialsProvider.builder()
                .processCommand("/bin/sleep 30")
                .timeoutMilliseconds(200)
                .build();
        try {
            timedOut.getCredentials();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("timed out after 200 milliseconds"));
        }

        ExternalCredentialsProvider badJson = ExternalCredentialsProvider.builder()
                .processCommand("/bin/echo not-json")
                .build();
        try {
            badJson.getCredentials();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("failed to parse external command output"));
        }

        ExternalCredentialsProvider empty = ExternalCredentialsProvider.builder()
                .processCommand("/bin/echo")
                .build();
        try {
            empty.getCredentials();
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("empty response")
                    || e.getMessage().contains("failed to parse external command output"));
        }
    }

    @Test
    public void testInvalidExpirationStillReturnsCredentials() {
        ExternalCredentialsProvider provider = ExternalCredentialsProvider.builder()
                .processCommand("/bin/echo '{\"mode\":\"AK\",\"access_key_id\":\"ak\",\"access_key_secret\":\"sk\",\"expiration\":\"not-a-date\"}'")
                .build();
        CredentialModel credential = provider.getCredentials();
        Assert.assertEquals("ak", credential.getAccessKeyId());
        Assert.assertEquals(0L, credential.getExpiration());
    }

    @Test
    public void testCallbackFailureDoesNotBreakRefresh() {
        ExternalCredentialsProvider provider = ExternalCredentialsProvider.builder()
                .processCommand("/bin/echo '{\"mode\":\"AK\",\"access_key_id\":\"ak\",\"access_key_secret\":\"sk\",\"expiration\":\"2049-10-20T04:27:09Z\"}'")
                .credentialUpdateCallback((accessKeyId, accessKeySecret, securityToken, expiration) -> {
                    throw new IllegalStateException("callback failed");
                })
                .build();
        Assert.assertEquals("ak", provider.getCredentials().getAccessKeyId());
        Assert.assertEquals("ak", provider.getCredentials().getAccessKeyId());
    }

    @Test
    public void testNearExpirationRefreshesAgain() {
        final AtomicInteger calls = new AtomicInteger();
        ExternalCredentialsProvider provider = ExternalCredentialsProvider.builder()
                .processCommand("/bin/echo '{\"mode\":\"StsToken\",\"access_key_id\":\"ak\",\"access_key_secret\":\"sk\",\"sts_token\":\"token\",\"expiration\":\"2000-01-01T00:00:00Z\"}'")
                .credentialUpdateCallback((accessKeyId, accessKeySecret, securityToken, expiration) -> calls.incrementAndGet())
                .build();
        provider.getCredentials();
        provider.getCredentials();
        Assert.assertEquals(2, calls.get());
    }
}
