package com.aliyun.credentials;

public class BearerTokenCredential implements AlibabaCloudCredentials {

    private String bearerToken;

    public BearerTokenCredential(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    @Override
    public String getAccessKeyId() {
        return null;
    }

    @Override
    public String getAccessKeySecret() {
        return null;
    }

    @Override
    public String getSecurityToken() {
        return null;
    }

    @Override
    public String getType() {
        return "bearer";
    }

    @Override
    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

}
