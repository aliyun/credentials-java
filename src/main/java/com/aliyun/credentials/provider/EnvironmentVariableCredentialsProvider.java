package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.utils.ProviderName;
import com.aliyun.credentials.utils.StringUtils;

public class EnvironmentVariableCredentialsProvider implements AlibabaCloudCredentialsProvider {
    @Override
    public CredentialModel getCredentials() {
        String accessKeyId = AuthUtils.getEnvironmentAccessKeyId();
        String accessKeySecret = AuthUtils.getEnvironmentAccessKeySecret();
        String securityToken = AuthUtils.getEnvironmentSecurityToken();
        if (StringUtils.isEmpty(accessKeyId)) {
            throw new CredentialException("Environment variable accessKeyId cannot be empty.");
        }
        if (StringUtils.isEmpty(accessKeySecret)) {
            throw new CredentialException("Environment variable accessKeySecret cannot be empty.");
        }
        if (!StringUtils.isEmpty(securityToken)) {
            return CredentialModel.builder()
                    .accessKeyId(accessKeyId)
                    .accessKeySecret(accessKeySecret)
                    .securityToken(securityToken)
                    .type(AuthConstant.STS)
                    .providerName(this.getProviderName())
                    .build();
        }
        return CredentialModel.builder()
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .type(AuthConstant.ACCESS_KEY)
                .providerName(this.getProviderName())
                .build();
    }

    @Override
    public String getProviderName() {
        return ProviderName.ENV;
    }

    @Override
    public void close() {
    }
}
