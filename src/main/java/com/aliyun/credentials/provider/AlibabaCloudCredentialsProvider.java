package com.aliyun.credentials.provider;

import com.aliyun.credentials.api.ICredentialsProvider;
import com.aliyun.credentials.models.CredentialModel;

public interface AlibabaCloudCredentialsProvider extends ICredentialsProvider {

    CredentialModel getCredentials();
}
