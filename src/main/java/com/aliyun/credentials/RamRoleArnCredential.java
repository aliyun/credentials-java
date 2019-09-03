package com.aliyun.credentials;

import com.aliyun.credentials.provider.AlibabaCloudCredentialsProvider;
import com.aliyun.credentials.utils.AuthConstant;

public class RamRoleArnCredential implements AlibabaCloudCredentials {

    private String accessKeyId;
    private String accessKeySecret;
    private String securityToken;
    private long expiration;
    private AlibabaCloudCredentialsProvider provider;

    public RamRoleArnCredential(String accessKeyId, String accessKeySecret, String securityToken, long expiration,
                                AlibabaCloudCredentialsProvider provider) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.securityToken = securityToken;
        this.expiration = expiration;
        this.provider = provider;
    }

    public boolean withShouldRefresh() {
        return System.currentTimeMillis() >= (this.expiration - 180);
    }

    public RamRoleArnCredential getNewRamRoleArnCredential() {
        try {
            return (RamRoleArnCredential) provider.getCredentials();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void refreshCredential() {
        if (withShouldRefresh()) {
            RamRoleArnCredential credential = getNewRamRoleArnCredential();
            this.expiration = credential.getExpiration();
            this.accessKeyId = credential.getAccessKeyId();
            this.accessKeySecret = credential.getAccessKeySecret();
            this.securityToken = credential.getSecurityToken();
        }
    }

    @Override
    public String getAccessKeyId() {
        refreshCredential();
        return accessKeyId;
    }

    @Override
    public String getAccessKeySecret() {
        refreshCredential();
        return accessKeySecret;
    }

    @Override
    public String getSecurityToken() {
        refreshCredential();
        return securityToken;
    }

    @Override
    public String getType() {
        return AuthConstant.RAM_ROLE_ARN;
    }

    public long getExpiration() {
        refreshCredential();
        return expiration;
    }

}
