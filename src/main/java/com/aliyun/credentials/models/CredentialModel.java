// This file is auto-generated, don't edit it. Thanks.
package com.aliyun.credentials.models;

import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.tea.*;

public class CredentialModel extends TeaModel implements AlibabaCloudCredentials {
    public String accessKeyId;
    public String accessKeySecret;
    public String securityToken;
    public String bearerToken;
    public String type;
    public long expiration;

    private CredentialModel(Builder builder) {
        this.accessKeyId = builder.accessKeyId;
        this.accessKeySecret = builder.accessKeySecret;
        this.securityToken = builder.securityToken;
        this.bearerToken = builder.bearerToken;
        this.type = builder.type;
        this.expiration = builder.expiration;
    }

    public static Builder builder() {
        return new Builder();
    }

    public CredentialModel setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
        return this;
    }

    @Override
    public String getAccessKeyId() {
        return this.accessKeyId;
    }

    public CredentialModel setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
        return this;
    }

    @Override
    public String getAccessKeySecret() {
        return this.accessKeySecret;
    }

    public CredentialModel setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
        return this;
    }

    @Override
    public String getSecurityToken() {
        return this.securityToken;
    }

    public CredentialModel setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String getType() {
        return this.type;
    }

    public long getExpiration() {
        return this.expiration;
    }

    public CredentialModel setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
        return this;
    }

    @Override
    public String getBearerToken() {
        return this.bearerToken;
    }

    public static final class Builder {
        private String accessKeyId;
        private String accessKeySecret;
        private String securityToken;
        private String bearerToken;
        private String type;
        private long expiration;

        public Builder accessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        public Builder accessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
            return this;
        }

        public Builder securityToken(String securityToken) {
            this.securityToken = securityToken;
            return this;
        }

        public Builder bearerToken(String bearerToken) {
            this.bearerToken = bearerToken;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder expiration(long expiration) {
            this.expiration = expiration;
            return this;
        }

        public CredentialModel build() {
            return new CredentialModel(this);
        }
    }

}

