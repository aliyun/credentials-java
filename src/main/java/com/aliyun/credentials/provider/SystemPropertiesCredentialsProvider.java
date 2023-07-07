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
        String accessKeySecret = System.getProperty(AuthConstant.SYSTEM_ACCESSKEYSECRET);
        if (StringUtils.isEmpty(accessKeyId) || StringUtils.isEmpty(accessKeySecret)) {
            return null;
        }
        return CredentialModel.builder()
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .build();
    }
}
