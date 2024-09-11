package com.aliyun.credentials;

import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.provider.*;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.StringUtils;
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
                            .disableIMDSv1(config.disableIMDSv1)
                            .connectionTimeout(config.connectTimeout)
                            .readTimeout(config.timeout)
                            .build();
                case AuthConstant.RAM_ROLE_ARN:
                    AlibabaCloudCredentialsProvider innerProvider;
                    if (StringUtils.isEmpty(config.securityToken)) {
                        innerProvider = StaticCredentialsProvider.builder()
                                .credential(CredentialModel.builder()
                                        .accessKeyId(Validate.notNull(
                                                config.accessKeyId, "AccessKeyId must not be null."))
                                        .accessKeySecret(Validate.notNull(
                                                config.accessKeySecret, "AccessKeySecret must not be null."))
                                        .type(AuthConstant.ACCESS_KEY)
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
                                        .build())
                                .build();
                    }
                    return RamRoleArnCredentialProvider.builder()
                            .credentialsProvider(innerProvider)
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
                case AuthConstant.CREDENTIALS_URI:
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

    /**
     * @return Access key ID
     * @deprecated Use getCredential().getAccessKeyId() instead of
     */
    @Deprecated
    public String getAccessKeyId() {
        return this.credentialsProvider.getCredentials().getAccessKeyId();
    }

    /**
     * @return Access key secret
     * @deprecated Use getCredential().getAccessKeySecret() instead of
     */
    @Deprecated
    public String getAccessKeySecret() {
        return this.credentialsProvider.getCredentials().getAccessKeySecret();
    }

    /**
     * @return Security token
     * @deprecated Use getCredential().getSecurityToken() instead of
     */
    @Deprecated
    public String getSecurityToken() {
        return this.credentialsProvider.getCredentials().getSecurityToken();
    }

    /**
     * @return Credentials provider type
     * @deprecated Use getCredential().getType() instead of
     */
    @Deprecated
    public String getType() {
        return this.credentialsProvider.getCredentials().getType();
    }

    /**
     * @return Bearer token
     * @deprecated Use getCredential().getBearerToken() instead of
     */
    @Deprecated
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

