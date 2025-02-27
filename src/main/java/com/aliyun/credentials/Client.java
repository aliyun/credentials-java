package com.aliyun.credentials;

import com.aliyun.credentials.api.ICredentialsProvider;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.provider.*;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.ProviderName;
import com.aliyun.credentials.utils.StringUtils;
import com.aliyun.tea.utils.Validate;

public class Client {
    private final ICredentialsProvider credentialsProvider;

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

    public Client(ICredentialsProvider provider) {
        this.credentialsProvider = provider;
    }

    private ICredentialsProvider getProvider(Config config) {
        switch (config.type) {
            case AuthConstant.ACCESS_KEY:
                return StaticCredentialsProvider.builder()
                        .credential(CredentialModel.builder()
                                .accessKeyId(Validate.notNull(
                                        config.accessKeyId, "AccessKeyId must not be null."))
                                .accessKeySecret(Validate.notNull(
                                        config.accessKeySecret, "AccessKeySecret must not be null."))
                                .type(config.type)
                                .providerName(ProviderName.STATIC_AK)
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
                                .providerName(ProviderName.STATIC_STS)
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
                        .disableIMDSv1(config.disableIMDSv1)
                        .connectionTimeout(config.connectTimeout)
                        .readTimeout(config.timeout)
                        .build();
            case AuthConstant.RAM_ROLE_ARN:
                ICredentialsProvider innerProvider;
                if (StringUtils.isEmpty(config.securityToken)) {
                    innerProvider = StaticCredentialsProvider.builder()
                            .credential(CredentialModel.builder()
                                    .accessKeyId(Validate.notNull(
                                            config.accessKeyId, "AccessKeyId must not be null."))
                                    .accessKeySecret(Validate.notNull(
                                            config.accessKeySecret, "AccessKeySecret must not be null."))
                                    .type(AuthConstant.ACCESS_KEY)
                                    .providerName(ProviderName.STATIC_AK)
                                    .build())
                            .build();
                } else {
                    innerProvider = StaticCredentialsProvider.builder()
                            .credential(CredentialModel.builder()
                                    .accessKeyId(Validate.notNull(
                                            config.accessKeyId, "AccessKeyId must not be null."))
                                    .accessKeySecret(Validate.notNull(
                                            config.accessKeySecret, "AccessKeySecret must not be null."))
                                    .securityToken(Validate.notNull(
                                            config.securityToken, "SecurityToken must not be null."))
                                    .type(AuthConstant.STS)
                                    .providerName(ProviderName.STATIC_STS)
                                    .build())
                            .build();
                }
                return RamRoleArnCredentialProvider.builder()
                        .credentialsProvider(innerProvider)
                        .durationSeconds(config.roleSessionExpiration)
                        .roleArn(config.roleArn)
                        .roleSessionName(config.roleSessionName)
                        .policy(config.policy)
                        .stsEndpoint(config.stsEndpoint)
                        .externalId(config.externalId)
                        .connectionTimeout(config.connectTimeout)
                        .readTimeout(config.timeout)
                        .build();
            case AuthConstant.RSA_KEY_PAIR:
                return RsaKeyPairCredentialProvider.builder()
                        .publicKeyId(config.publicKeyId)
                        .privateKeyFile(config.privateKeyFile)
                        .durationSeconds(config.roleSessionExpiration)
                        .stsEndpoint(config.stsEndpoint)
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
                        .stsEndpoint(config.stsEndpoint)
                        .connectionTimeout(config.connectTimeout)
                        .readTimeout(config.timeout)
                        .build();
            case AuthConstant.CREDENTIALS_URI:
                return URLCredentialProvider.builder()
                        .credentialsURI(config.credentialsURI)
                        .connectionTimeout(config.connectTimeout)
                        .readTimeout(config.timeout)
                        .build();
            default:
                throw new CredentialException("invalid type option, support: access_key, sts, ecs_ram_role, ram_role_arn, rsa_key_pair");
        }
    }
    /**
     * Get credential
     *
     * @return the whole credential
     */
    public CredentialModel getCredential() {
        return (CredentialModel)this.credentialsProvider.getCredentials();
    }
}

