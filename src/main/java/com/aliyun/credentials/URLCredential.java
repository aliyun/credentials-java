package com.aliyun.credentials;

import com.aliyun.credentials.provider.AlibabaCloudCredentialsProvider;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.RefreshUtils;
import com.aliyun.credentials.models.Credential;

@Deprecated
public class URLCredential implements AlibabaCloudCredentials {

    private String accessKeyId;
    private String accessKeySecret;
    private String securityToken;
    private long expiration;
    private AlibabaCloudCredentialsProvider provider;

    public URLCredential(String accessKeyId, String accessKeySecret, String securityToken, long expiration,
                         AlibabaCloudCredentialsProvider provider) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.securityToken = securityToken;
        this.expiration = expiration;
        this.provider = provider;
    }

    public void refreshCredential() {
        if (RefreshUtils.withShouldRefresh(this.expiration)) {
            Credential credential = (Credential) RefreshUtils.getNewCredential(this.provider);
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
        return AuthConstant.URL_STS;
    }

    @Override
    public String getBearerToken() {
        return null;
    }

    public long getExpiration() {
        refreshCredential();
        return expiration;
    }

}
