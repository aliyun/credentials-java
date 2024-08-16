package com.aliyun.credentials.provider;

import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.utils.StringUtils;

public class SystemPropertiesCredentialsProvider implements AlibabaCloudCredentialsProvider {
    @Override
    public CredentialModel getCredentials() {
        if (!"default".equals(AuthUtils.getClientType())) {
            return null;
        }
        String accessKeyId = System.getProperty(AuthConstant.SYSTEM_ACCESSKEYID);
        String accessKeySecret = System.getProperty(AuthConstant.SYSTEM_ACCESSKEY_SECRET);
        if (!StringUtils.isEmpty(System.getProperty(AuthConstant.SYSTEM_ACCESSKEYSECRET))) {
            accessKeySecret = System.getProperty(AuthConstant.SYSTEM_ACCESSKEYSECRET);
        }
        String securityToken = System.getProperty(AuthConstant.SYSTEM_SESSION_TOKEN);
        if (StringUtils.isEmpty(accessKeyId) ||  StringUtils.isEmpty(accessKeySecret)) {
            return null;
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
