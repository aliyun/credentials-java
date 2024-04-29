package com.aliyun.credentials;

import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.provider.*;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.tea.utils.Validate;

public class Client {
    private final AlibabaCloudCredentialsProvider credentialsProvider;

    public Client() {
        this.credentialsProvider = new DefaultCredentialsProvider();
    }

    public Client(Config config) {
        if (null == config) {
            this.credentialsProvider = new DefaultCredentialsProvider();
        } else {
            this.credentialsProvider = getProvider(config);
        }
    }

    public Client(AlibabaCloudCredentialsProvider provider) {
        this.credentialsProvider = provider;
    }

    private AlibabaCloudCredentialsProvider getProvider(Config config) {
        try {
            switch (config.type) {
                case AuthConstant.ACCESS_KEY:
                    return StaticCredentialsProvider.builder()
                            .credential(CredentialModel.builder()
                                    .accessKeyId(Validate.notNull(
                                            config.accessKeyId, "AccessKeyId must not be null."))
                                    .accessKeySecret(Validate.notNull(
                                            config.accessKeySecret, "AccessKeySecret must not be null."))
                                    .type(config.type)
                                    .build())
                            .build();
                case AuthConstant.STS:
                    return StaticCredentialsProvider.builder()
                            .credential(CredentialModel.builder()
                                    .accessKeyId(Validate.notNull(
                                            config.accessKeyId, "AccessKeyId must not be null."))
                                    .accessKeySecret(Validate.notNull(
                                            config.accessKeySecret, "AccessKeySecret must not be null."))
                                    .securityToken(Validate.notNull(
                                            config.securityToken, "SecurityToken must not be null."))
                                    .type(config.type)
                                    .build())
                            .build();
                case AuthConstant.BEARER:
                    return StaticCredentialsProvider.builder()
                            .credential(CredentialModel.builder()
                                    .bearerToken(Validate.notNull(
                                            config.bearerToken, "BearerToken must not be null."))
                                    .type(config.type)
                                    .build())
                            .build();
                case AuthConstant.ECS_RAM_ROLE:
                    return EcsRamRoleCredentialProvider.builder()
                            .roleName(config.roleName)
                            .enableIMDSv2(config.enableIMDSv2)
                            .metadataTokenDuration(config.metadataTokenDuration)
                            .connectionTimeout(config.connectTimeout)
                            .readTimeout(config.timeout)
                            .build();
                case AuthConstant.RAM_ROLE_ARN:
                    return RamRoleArnCredentialProvider.builder()
                            .accessKeyId(config.accessKeyId)
                            .accessKeySecret(config.accessKeySecret)
                            .durationSeconds(config.roleSessionExpiration)
                            .roleArn(config.roleArn)
                            .roleSessionName(config.roleSessionName)
                            .policy(config.policy)
                            .STSEndpoint(config.STSEndpoint)
                            .externalId(config.externalId)
                            .connectionTimeout(config.connectTimeout)
                            .readTimeout(config.timeout)
                            .build();
                case AuthConstant.RSA_KEY_PAIR:
                    return RsaKeyPairCredentialProvider.builder()
                            .publicKeyId(config.publicKeyId)
                            .privateKeyFile(config.privateKeyFile)
                            .durationSeconds(config.roleSessionExpiration)
                            .STSEndpoint(config.STSEndpoint)
                            .connectionTimeout(config.connectTimeout)
                            .readTimeout(config.timeout)
                            .build();
                case AuthConstant.OIDC_ROLE_ARN:
                    return OIDCRoleArnCredentialProvider.builder()
                            .durationSeconds(config.roleSessionExpiration)
                            .roleArn(config.roleArn)
                            .roleSessionName(config.roleSessionName)
                            .oidcProviderArn(config.oidcProviderArn)
                            .oidcTokenFilePath(config.oidcTokenFilePath)
                            .policy(config.policy)
                            .STSEndpoint(config.STSEndpoint)
                            .connectionTimeout(config.connectTimeout)
                            .readTimeout(config.timeout)
                            .build();
                case AuthConstant.URL_STS:
                    return URLCredentialProvider.builder()
                            .credentialsURI(config.credentialsURI)
                            .connectionTimeout(config.connectTimeout)
                            .readTimeout(config.timeout)
                            .build();
                default:
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultCredentialsProvider();
    }

    public String getAccessKeyId() {
        return this.credentialsProvider.getCredentials().getAccessKeyId();
    }

    public String getAccessKeySecret() {
        return this.credentialsProvider.getCredentials().getAccessKeySecret();
    }

    public String getSecurityToken() {
        return this.credentialsProvider.getCredentials().getSecurityToken();
    }

    public String getType() {
        return this.credentialsProvider.getCredentials().getType();
    }

    public String getBearerToken() {
        return this.credentialsProvider.getCredentials().getBearerToken();
    }

    /**
     * Get credential
     *
     * @return the whole credential
     */
    public CredentialModel getCredential() {
        return this.credentialsProvider.getCredentials();
    }
}

