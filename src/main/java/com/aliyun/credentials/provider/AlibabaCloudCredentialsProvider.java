package com.aliyun.credentials.provider;

import com.aliyun.credentials.models.CredentialModel;

public interface AlibabaCloudCredentialsProvider {

    CredentialModel getCredentials();
}
