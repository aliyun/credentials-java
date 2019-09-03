package com.aliyun.credentials;

import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.ParameterHelper;

import java.text.ParseException;

public class EcsRamRoleCredential implements AlibabaCloudCredentials {

    private long expiration;
    private String accessKeyId;
    private String accessKeySecret;
    private String securityToken;

    public EcsRamRoleCredential() {
    }

    public EcsRamRoleCredential(String accessKeyId, String accessKeySecret, String securityToken, String expiration) throws ParseException {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.securityToken = securityToken;
        this.expiration = ParameterHelper.getUTCDate(expiration).getTime();
    }

    @Override
    public String getAccessKeyId() {
        return accessKeyId;
    }

    @Override
    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    @Override
    public String getSecurityToken() {
        return securityToken;
    }

    @Override
    public String getType() {
        return AuthConstant.ECS_RAM_ROLE;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}
