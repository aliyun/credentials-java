package com.aliyun.credentials.provider;

import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.ProviderName;
import org.junit.Assert;
import org.junit.Test;

public class StaticCredentialsProviderTest {
    @Test
    public void getCredentialsTest() {
        StaticCredentialsProvider provider = StaticCredentialsProvider.builder().build();
        Assert.assertNull(provider.getProviderName());

        provider = StaticCredentialsProvider.builder()
                .credential(CredentialModel.builder()
                        .accessKeyId("test")
                        .accessKeySecret("test")
                        .securityToken("test")
                        .type(AuthConstant.STS)
                        .providerName(ProviderName.STATIC_STS)
                        .build())
                .build();
        Assert.assertEquals(ProviderName.STATIC_STS, provider.getProviderName());
        CredentialModel credentialModel = provider.getCredentials();
        Assert.assertEquals(ProviderName.STATIC_STS, credentialModel.getProviderName());
        Assert.assertEquals("test", credentialModel.getAccessKeyId());
        Assert.assertEquals("test", credentialModel.getAccessKeySecret());
        Assert.assertEquals("test", credentialModel.getSecurityToken());
        Assert.assertEquals("sts", credentialModel.getType());

        provider = StaticCredentialsProvider.builder()
                .credential(CredentialModel.builder()
                        .accessKeyId("test")
                        .accessKeySecret("test")
                        .type(AuthConstant.ACCESS_KEY)
                        .providerName(ProviderName.STATIC_AK)
                        .build())
                .build();
        Assert.assertEquals(ProviderName.STATIC_AK, provider.getProviderName());
        credentialModel = provider.getCredentials();
        Assert.assertEquals(ProviderName.STATIC_AK, credentialModel.getProviderName());
        Assert.assertEquals("test", credentialModel.getAccessKeyId());
        Assert.assertEquals("test", credentialModel.getAccessKeySecret());
        Assert.assertEquals("access_key", credentialModel.getType());
        provider.close();
    }
}
