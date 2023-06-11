package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.Credential;
import com.aliyun.credentials.utils.AuthUtils;

public class EnvironmentVariableCredentialsProvider implements AlibabaCloudCredentialsProvider {
    @Override
    public Credential getCredentials() {
        if (!"default".equals(AuthUtils.getClientType())) {
            return null;
        }

        String accessKeyId = AuthUtils.getEnvironmentAccessKeyId();
        String accessKeySecret = AuthUtils.getEnvironmentAccessKeySecret();
        if (accessKeyId == null || accessKeySecret == null) {
            return null;
        }
        if (accessKeyId.length() == 0) {
            throw new CredentialException("Environment variable accessKeyId cannot be empty");
        }
        if (accessKeySecret.length() == 0) {
            throw new CredentialException("Environment variable accessKeySecret cannot be empty");
        }
        return Credential.builder()
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .build();
    }
}
