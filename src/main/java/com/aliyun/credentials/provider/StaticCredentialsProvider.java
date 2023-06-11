package com.aliyun.credentials.provider;

import com.aliyun.credentials.models.Credential;

public class StaticCredentialsProvider implements AlibabaCloudCredentialsProvider {
    private Credential credential;

    private StaticCredentialsProvider(Builder builder) {
        this.credential = builder.credential;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Credential getCredentials() {
        return this.credential;
    }

    public static final class Builder {
        private Credential credential;

        public Builder credential(Credential credential) {
            this.credential = credential;
            return this;
        }

        public StaticCredentialsProvider build() {
            return new StaticCredentialsProvider(this);
        }
    }
}
