package com.aliyun.credentials;

import com.aliyun.credentials.utils.AuthConstant;

@Deprecated
public class StsCredential implements AlibabaCloudCredentials {

    private String accessKeyId;
    private String accessKeySecret;
    private String securityToken;

    public StsCredential() {
    }

    public StsCredential(String accessKeyId, String accessKeySecret, String securityToken) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.securityToken = securityToken;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    @Override
    public String getAccessKeyId() {
        return this.accessKeyId;
    }

    @Override
    public String getAccessKeySecret() {
        return this.accessKeySecret;
    }

    @Override
    public String getSecurityToken() {
        return this.securityToken;
    }

    public void setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
    }

    @Override
    public String getType() {
        return AuthConstant.STS;
    }

    @Override
    public String getBearerToken() {
        return null;
    }
}
