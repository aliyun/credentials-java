package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.utils.StringUtils;

public class EnvironmentVariableCredentialsProvider implements AlibabaCloudCredentialsProvider {
    @Override
    public CredentialModel getCredentials() {
        if (!"default".equals(AuthUtils.getClientType())) {
            return null;
        }

        String accessKeyId = AuthUtils.getEnvironmentAccessKeyId();
        String accessKeySecret = AuthUtils.getEnvironmentAccessKeySecret();
        String securityToken = AuthUtils.getEnvironmentSecurityToken();
        if (accessKeyId == null || accessKeySecret == null) {
            return null;
        }
        if (accessKeyId.length() == 0) {
            throw new CredentialException("Environment variable accessKeyId cannot be empty");
        }
        if (accessKeySecret.length() == 0) {
            throw new CredentialException("Environment variable accessKeySecret cannot be empty");
        }
        if (!StringUtils.isEmpty(securityToken)) {
            return CredentialModel.builder()
                    .accessKeyId(accessKeyId)
                    .accessKeySecret(accessKeySecret)
                    .securityToken(securityToken)
                    .type(AuthConstant.STS)
                    .build();
        }
        return CredentialModel.builder()
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .type(AuthConstant.ACCESS_KEY)
                .build();
    }
}
