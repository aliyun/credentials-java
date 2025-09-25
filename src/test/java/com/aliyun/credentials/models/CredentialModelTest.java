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

    @Test
    public void toStringWithBearerTokenTest() {
        CredentialModel model = CredentialModel.builder()
                .bearerToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")
                .providerName("testProvider")
                .build();

        String result = model.toString();
        Assert.assertTrue("toString should contain bearerToken format", result.startsWith("Credential(bearerToken="));
        Assert.assertTrue("toString should contain masked bearerToken", result.contains("eyJ******************************CJ9"));
        Assert.assertTrue("toString should contain providerName", result.contains("providerName=testProvider"));
        Assert.assertFalse("toString should not contain accessKeyId when bearerToken exists", result.contains("accessKeyId"));
    }

    @Test
    public void toStringWithoutBearerTokenTest() {
        CredentialModel model = CredentialModel.builder()
                .accessKeyId("LTAI4G3jVkK2DYajXXXXXXXX")
                .accessKeySecret("abcdefghijklmnopqrstuvwxyz")
                .securityToken("STS.NUCJMaR7bKxD5A2rDq6")
                .providerName("testProvider")
                .expiration(1234567890L)
                .build();

        String result = model.toString();
        Assert.assertTrue("toString should contain accessKeyId format", result.startsWith("Credential(accessKeyId="));
        Assert.assertTrue("toString should contain masked accessKeyId", result.contains("LTA******************XXX"));
        Assert.assertTrue("toString should contain masked accessKeySecret", result.contains("abc********************xyz"));
        Assert.assertTrue("toString should contain masked securityToken", result.contains("STS*****************Dq6"));
        Assert.assertTrue("toString should contain providerName", result.contains("providerName=testProvider"));
        Assert.assertTrue("toString should contain expiration", result.contains("expiration=1234567890"));
    }

    @Test
    public void toStringWithEmptyBearerTokenTest() {
        CredentialModel model = CredentialModel.builder()
                .accessKeyId("testAccessKeyId")
                .accessKeySecret("testAccessKeySecret")
                .securityToken("testSecurityToken")
                .bearerToken("")  // 空字符串
                .providerName("testProvider")
                .expiration(9876543210L)
                .build();

        String result = model.toString();
        // 空字符串应该被认为是空，所以应该使用accessKey格式
        Assert.assertTrue("toString should contain accessKeyId format when bearerToken is empty", result.startsWith("Credential(accessKeyId="));
        Assert.assertTrue("toString should contain masked accessKeyId", result.contains("tes*********yId"));
        Assert.assertFalse("toString should not contain bearerToken when it's empty", result.contains("bearerToken"));
    }

    @Test
    public void toStringWithNullBearerTokenTest() {
        CredentialModel model = CredentialModel.builder()
                .accessKeyId("testAccessKeyId")
                .accessKeySecret("testAccessKeySecret")
                .securityToken("testSecurityToken")
                .bearerToken(null)  // null值
                .providerName("testProvider")
                .expiration(9876543210L)
                .build();

        String result = model.toString();
        // null应该被认为是空，所以应该使用accessKey格式
        Assert.assertTrue("toString should contain accessKeyId format when bearerToken is null", result.startsWith("Credential(accessKeyId="));
        Assert.assertTrue("toString should contain masked accessKeyId", result.contains("tes*********yId"));
        Assert.assertFalse("toString should not contain bearerToken when it's null", result.contains("bearerToken"));
    }

    @Test
    public void toStringMaskingShortValuesTest() {
        CredentialModel model = CredentialModel.builder()
                .accessKeyId("short")  // 短字符串
                .accessKeySecret("abc")  // 很短的字符串
                .securityToken(null)  // null值
                .providerName("testProvider")
                .expiration(1111111111L)
                .build();

        String result = model.toString();
        Assert.assertTrue("toString should mask short accessKeyId with ****", result.contains("accessKeyId=****"));
        Assert.assertTrue("toString should mask short accessKeySecret with ****", result.contains("accessKeySecret=****"));
        Assert.assertTrue("toString should mask null securityToken with ****", result.contains("securityToken=****"));
    }
}