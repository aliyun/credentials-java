package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.ProviderName;
import com.aliyun.credentials.utils.StringUtils;

public class SystemPropertiesCredentialsProvider implements AlibabaCloudCredentialsProvider {
    @Override
    public CredentialModel getCredentials() {
        String accessKeyId = System.getProperty(AuthConstant.SYSTEM_ACCESSKEYID);
        String accessKeySecret = System.getProperty(AuthConstant.SYSTEM_ACCESSKEY_SECRET);
        if (!StringUtils.isEmpty(System.getProperty(AuthConstant.SYSTEM_ACCESSKEYSECRET))) {
            accessKeySecret = System.getProperty(AuthConstant.SYSTEM_ACCESSKEYSECRET);
        }
        String securityToken = System.getProperty(AuthConstant.SYSTEM_SESSION_TOKEN);
        if (StringUtils.isEmpty(accessKeyId)) {
            throw new CredentialException("System property alibabacloud.accessKeyId cannot be empty.");
        }
        if (StringUtils.isEmpty(accessKeySecret)) {
            throw new CredentialException("System property alibabacloud.accessKeySecret cannot be empty.");
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
        return ProviderName.SYSTEM;
    }

    @Override
    public void close() {
    }
}
