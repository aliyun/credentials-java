package com.aliyun.credentials.provider;

public class CredentialsProviderFactory {
    public <T extends AlibabaCloudCredentialsProvider> T createCredentialsProvider(T classInstance) {
        return classInstance;
    }
}
