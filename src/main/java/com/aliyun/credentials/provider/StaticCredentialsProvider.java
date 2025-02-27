package com.aliyun.credentials.provider;

import com.aliyun.credentials.api.ICredentialsProvider;
import com.aliyun.credentials.models.CredentialModel;

public class StaticCredentialsProvider implements ICredentialsProvider {
    private CredentialModel credential;

    private StaticCredentialsProvider(Builder builder) {
        this.credential = builder.credential;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CredentialModel getCredentials() {
        return this.credential;
    }

    public static final class Builder {
        private CredentialModel credential;

        public Builder credential(CredentialModel credential) {
            this.credential = credential;
            return this;
        }

        public StaticCredentialsProvider build() {
            return new StaticCredentialsProvider(this);
        }
    }

    @Override
    public String getProviderName() {
        return this.credential != null ? this.credential.getProviderName() : null;
    }

    @Override
    public void close() {
    }
}
