package com.aliyun.credentials;

import com.aliyun.credentials.provider.AlibabaCloudCredentialsProvider;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.ParameterHelper;
import com.aliyun.credentials.utils.RefreshUtils;
import com.aliyun.credentials.models.Credential;

@Deprecated
public class EcsRamRoleCredential implements AlibabaCloudCredentials {

    private long expiration;
    private String accessKeyId;
    private String accessKeySecret;
    private String securityToken;
    private AlibabaCloudCredentialsProvider provider;

    public EcsRamRoleCredential() {
    }

    public EcsRamRoleCredential(String accessKeyId, String accessKeySecret, String securityToken, String expiration,
                                AlibabaCloudCredentialsProvider provider) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.securityToken = securityToken;
        this.provider = provider;
        this.expiration = ParameterHelper.getUTCDate(expiration).getTime();
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
        return AuthConstant.ECS_RAM_ROLE;
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
