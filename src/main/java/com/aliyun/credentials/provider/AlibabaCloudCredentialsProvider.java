package com.aliyun.credentials.provider;

import com.aliyun.credentials.models.Credential;

public interface AlibabaCloudCredentialsProvider {

    Credential getCredentials();
}
