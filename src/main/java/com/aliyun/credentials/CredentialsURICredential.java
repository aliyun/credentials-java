package com.aliyun.credentials;

import com.aliyun.credentials.provider.AlibabaCloudCredentialsProvider;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.ParameterHelper;
import com.aliyun.credentials.utils.RefreshUtils;

public class CredentialsURICredential implements AlibabaCloudCredentials {

    private long expiration;
    private String accessKeyId;
    private String accessKeySecret;
    private String securityToken;
    private AlibabaCloudCredentialsProvider provider;

    public CredentialsURICredential() {
    }

    public CredentialsURICredential(String accessKeyId, String accessKeySecret, String securityToken, String expiration,
                                AlibabaCloudCredentialsProvider provider) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.securityToken = securityToken;
        this.provider = provider;
        this.expiration = ParameterHelper.getUTCDate(expiration).getTime();
    }

    public void refreshCredential() {
        if (RefreshUtils.withShouldRefresh(this.expiration)) {
            CredentialsURICredential credential = (CredentialsURICredential) RefreshUtils.getNewCredential(this.provider);
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
        return AuthConstant.CREDENTIALS_URI;
    }

    @Override
    public String getBearerToken() {
        return null;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}
