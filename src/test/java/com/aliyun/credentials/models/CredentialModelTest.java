package com.aliyun.credentials.models;

import org.junit.Assert;
import org.junit.Test;

public class CredentialModelTest {
    @Test
    public void builderTest() {
        CredentialModel model = CredentialModel.builder()
            .accessKeyId("akid")
            .accessKeySecret("aksecret")
            .securityToken("securityToken")
            .bearerToken("bearertoken")
            .expiration(100L)
            .type("type")
            .build();
        Assert.assertEquals("akid", model.getAccessKeyId());
        Assert.assertEquals("aksecret", model.getAccessKeySecret());
        Assert.assertEquals("securityToken", model.getSecurityToken());
        Assert.assertEquals("bearertoken", model.getBearerToken());
        Assert.assertEquals(100L, model.getExpiration());
        Assert.assertEquals("type", model.getType());
    }

    @Test
    public void setGetTest() {
        CredentialModel model = CredentialModel.builder().build();
        model.setAccessKeyId("akid")
            .setAccessKeySecret("aksecret")
            .setSecurityToken("securityToken")
            .setBearerToken("bearertoken")
            .setType("type");
        Assert.assertEquals("akid", model.getAccessKeyId());
        Assert.assertEquals("aksecret", model.getAccessKeySecret());
        Assert.assertEquals("securityToken", model.getSecurityToken());
        Assert.assertEquals("bearertoken", model.getBearerToken());
        // TODO: no setExpiration() method
        // Assert.assertEquals(100L, model.getExpiration());
        Assert.assertEquals("type", model.getType());
    }
}
