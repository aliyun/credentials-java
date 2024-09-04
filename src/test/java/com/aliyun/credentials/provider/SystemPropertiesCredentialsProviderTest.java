package com.aliyun.credentials.provider;

import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.utils.AuthConstant;
import org.junit.Assert;
import org.junit.Test;

public class SystemPropertiesCredentialsProviderTest {

    @Test
    public void getCredentialsTest() {
        SystemPropertiesCredentialsProvider provider = new SystemPropertiesCredentialsProvider();
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("System property alibabacloud.accessKeyId cannot be empty.", e.getMessage());
        }

        System.setProperty(AuthConstant.SYSTEM_ACCESSKEYSECRET, "");
        System.setProperty(AuthConstant.SYSTEM_ACCESSKEYID, "accessKeyIdTest");
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("System property alibabacloud.accessKeySecret cannot be empty.", e.getMessage());
        }

        System.setProperty(AuthConstant.SYSTEM_ACCESSKEY_SECRET, "accessKeyIdTest");
        AlibabaCloudCredentials credential = provider.getCredentials();
        String accessKeyId = credential.getAccessKeyId();
        String accessKeySecret = credential.getAccessKeySecret();
        Assert.assertEquals("accessKeyIdTest", accessKeyId);
        Assert.assertEquals("accessKeyIdTest", accessKeySecret);
        Assert.assertNull(credential.getSecurityToken());
        Assert.assertEquals("access_key", credential.getType());

        System.setProperty(AuthConstant.SYSTEM_ACCESSKEYSECRET, "accessKeySecretTest");
        credential = provider.getCredentials();
        accessKeyId = credential.getAccessKeyId();
        accessKeySecret = credential.getAccessKeySecret();
        Assert.assertEquals("accessKeyIdTest", accessKeyId);
        Assert.assertEquals("accessKeySecretTest", accessKeySecret);
        Assert.assertNull(credential.getSecurityToken());
        Assert.assertEquals("access_key", credential.getType());

        System.setProperty(AuthConstant.SYSTEM_SESSION_TOKEN, "sessionTokenTest");
        credential = provider.getCredentials();
        accessKeyId = credential.getAccessKeyId();
        accessKeySecret = credential.getAccessKeySecret();
        String sessionToken = credential.getSecurityToken();
        Assert.assertEquals("accessKeyIdTest", accessKeyId);
        Assert.assertEquals("accessKeySecretTest", accessKeySecret);
        Assert.assertEquals("sessionTokenTest", sessionToken);

        System.setProperty(AuthConstant.SYSTEM_ACCESSKEYID, "");
        try {
            provider.getCredentials();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("System property alibabacloud.accessKeyId cannot be empty.", e.getMessage());
        }

        System.setProperty(AuthConstant.SYSTEM_ACCESSKEYSECRET, "");
        System.setProperty(AuthConstant.SYSTEM_ACCESSKEY_SECRET, "");
        System.setProperty(AuthConstant.SYSTEM_SESSION_TOKEN, "");
    }
}
