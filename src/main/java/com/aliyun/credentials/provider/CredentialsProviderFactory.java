package com.aliyun.credentials.provider;

import com.aliyun.credentials.api.ICredentialsProvider;

public class CredentialsProviderFactory {
    public <T extends ICredentialsProvider> T createCredentialsProvider(T classInstance) {
        return classInstance;
    }
}
