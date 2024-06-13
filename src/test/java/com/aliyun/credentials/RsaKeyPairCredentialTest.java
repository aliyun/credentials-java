package com.aliyun.credentials;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.provider.RamRoleArnCredentialProvider;
import com.aliyun.credentials.provider.RsaKeyPairCredentialProvider;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class RsaKeyPairCredentialTest {
    @Test
    public void rsaKeyPairCredentialTest() {
        RsaKeyPairCredential credential = new RsaKeyPairCredential("id", "secret",
                7972517532000L, new RamRoleArnCredentialProvider("test", "test", "test"));

        Assert.assertEquals("id", credential.getAccessKeyId());
        Assert.assertEquals("secret", credential.getAccessKeySecret());
        Assert.assertEquals(7972517532000L, credential.getExpiration());
        Assert.assertNull(credential.getSecurityToken());
        Assert.assertEquals(AuthConstant.RSA_KEY_PAIR, credential.getType());
        Assert.assertTrue(credential.getProvider() instanceof RamRoleArnCredentialProvider);

        try {
            new RsaKeyPairCredential(null, null, 0, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("You must provide a valid pair of Public Key ID and Private Key Secret.",
                    e.getMessage());
        }

        try {
            new RsaKeyPairCredential("test", null, 0, null);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("You must provide a valid pair of Public Key ID and Private Key Secret.",
                    e.getMessage());
        }
    }

    @Test
    public void refreshCredentialTest() {
        RsaKeyPairCredentialProvider provider = Mockito.mock(RsaKeyPairCredentialProvider.class);
        CredentialModel credential = CredentialModel.builder()
                .accessKeyId("result")
                .accessKeySecret("test")
                .type(AuthConstant.RSA_KEY_PAIR)
                .expiration(7972517532000L)
                .build();
        Mockito.when(provider.getCredentials()).thenReturn(credential);

        RsaKeyPairCredential newCredential = new RsaKeyPairCredential("id", "secret",
                1000L, provider);
        Assert.assertEquals("result", newCredential.getAccessKeyId());
    }

}
