package com.aliyun.credentials;

import com.aliyun.credentials.provider.AlibabaCloudCredentialsProvider;
import com.aliyun.credentials.utils.AuthConstant;

public class RsaKeyPairCredential implements AlibabaCloudCredentials {

    private String privateKeySecret;
    private String publicKeyId;
    private long expiration;
    private AlibabaCloudCredentialsProvider provider;

    public RsaKeyPairCredential(String publicKeyId, String privateKeySecret, long expiration, AlibabaCloudCredentialsProvider provider) {
        if (publicKeyId == null || privateKeySecret == null) {
            throw new IllegalArgumentException(
                    "You must provide a valid pair of Public Key ID and Private Key Secret.");
        }

        this.publicKeyId = publicKeyId;
        this.privateKeySecret = privateKeySecret;
        this.expiration = expiration;
        this.provider = provider;
    }

    @Override
    public String getAccessKeyId() {
        return publicKeyId;
    }

    @Override
    public String getAccessKeySecret() {
        return privateKeySecret;
    }

    @Override
    public String getSecurityToken() {
        return null;
    }

    @Override
    public String getType() {
        return AuthConstant.RSA_KEY_PAIR;
    }

    @Override
    public String getBearerToken() {
        return null;
    }

    public long getExpiration() {
        return expiration;
    }

    public AlibabaCloudCredentialsProvider getProvider() {
        return provider;
    }
}
