package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileCredentialsProvider implements AlibabaCloudCredentialsProvider {
    private static volatile Map<String, Map<String, String>> ini;

    private static Map<String, Map<String, String>> getIni(String filePath) throws IOException {
        if (null == ini) {
            ini = ProfileUtils.parseFile(filePath);
        }
        return ini;
    }

    @Override
    public CredentialModel getCredentials() {
        String filePath = AuthUtils.getEnvironmentCredentialsFile();
        if (filePath == null) {
            filePath = AuthConstant.DEFAULT_CREDENTIALS_FILE_PATH;
        }
        if (filePath.length() == 0) {
            throw new CredentialException("The specified credentials file is empty.");
        }
        Map<String, Map<String, String>> ini;
        try {
            ini = getIni(filePath);
        } catch (IOException e) {
            throw new CredentialException(String.format("Unable to open credentials file: %s.", filePath));
        }
        Map<String, Map<String, String>> client = loadIni(ini);
        Map<String, String> clientConfig = client.get(AuthUtils.getClientType());
        if (clientConfig == null) {
            throw new CredentialException("Client is not open in the specified credentials file.");
        }
        CredentialsProviderFactory credentialsProviderFactory = new CredentialsProviderFactory();
        return createCredential(clientConfig, credentialsProviderFactory);
    }

    private Map<String, Map<String, String>> loadIni(Map<String, Map<String, String>> ini) {
        Map<String, Map<String, String>> client = new HashMap<String, Map<String, String>>(16);
        String enable;
        for (Map.Entry<String, Map<String, String>> clientType : ini.entrySet()) {
            enable = clientType.getValue().get(AuthConstant.INI_ENABLE);
            if (Boolean.parseBoolean(enable)) {
                Map<String, String> clientConfig = new HashMap<String, String>(16);
                for (Map.Entry<String, String> enabledClient : clientType.getValue().entrySet()) {
                    clientConfig.put(enabledClient.getKey(), enabledClient.getValue());
                }
                client.put(clientType.getKey(), clientConfig);
            }
        }
        return client;
    }

    private CredentialModel createCredential(Map<String, String> clientConfig,
                                             CredentialsProviderFactory factory) {
        String configType = clientConfig.get(AuthConstant.INI_TYPE);
        if (StringUtils.isEmpty(configType)) {
            throw new CredentialException("The configured client type is empty.");
        }
        if (AuthConstant.INI_TYPE_ARN.equals(configType)) {
            return getSTSAssumeRoleSessionCredentials(clientConfig, factory);
        }
        if (AuthConstant.INI_TYPE_KEY_PAIR.equals(configType)) {
            return getSTSGetSessionAccessKeyCredentials(clientConfig, factory);
        }
        if (AuthConstant.INI_TYPE_RAM.equals(configType)) {
            return getInstanceProfileCredentials(clientConfig, factory);
        }
        if (AuthConstant.INI_TYPE_OIDC.equals(configType)) {
            return getSTSOIDCRoleSessionCredentials(clientConfig, factory);
        }
        String accessKeyId = clientConfig.get(AuthConstant.INI_ACCESS_KEY_ID);
        String accessKeySecret = clientConfig.get(AuthConstant.INI_ACCESS_KEY_IDSECRET);
        if (StringUtils.isEmpty(accessKeyId) || StringUtils.isEmpty(accessKeySecret)) {
            throw new CredentialException("The configured access_key_id or access_key_secret is empty.");
        }
        return CredentialModel.builder()
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .type(AuthConstant.ACCESS_KEY)
                .providerName(String.format("%s/%s", this.getProviderName(), ProviderName.STATIC_AK))
                .build();
    }

    private CredentialModel getSTSAssumeRoleSessionCredentials(Map<String, String> clientConfig,
                                                               CredentialsProviderFactory factory) {
        String accessKeyId = clientConfig.get(AuthConstant.INI_ACCESS_KEY_ID);
        String accessKeySecret = clientConfig.get(AuthConstant.INI_ACCESS_KEY_IDSECRET);
        String roleSessionName = clientConfig.get(AuthConstant.INI_ROLE_SESSION_NAME);
        String roleArn = clientConfig.get(AuthConstant.INI_ROLE_ARN);
        String regionId = clientConfig.get(AuthConstant.DEFAULT_REGION);
        String policy = clientConfig.get(AuthConstant.INI_POLICY);
        if (StringUtils.isEmpty(accessKeyId) || StringUtils.isEmpty(accessKeySecret)) {
            throw new CredentialException("The configured access_key_id or access_key_secret is empty.");
        }
        if (StringUtils.isEmpty(roleSessionName) || StringUtils.isEmpty(roleArn)) {
            throw new CredentialException("The configured role_session_name or role_arn is empty.");
        }
        RamRoleArnCredentialProvider provider = factory.createCredentialsProvider(
                RamRoleArnCredentialProvider.builder()
                        .accessKeyId(accessKeyId)
                        .accessKeySecret(accessKeySecret)
                        .roleArn(roleArn)
                        .roleSessionName(roleSessionName)
                        .regionId(regionId)
                        .policy(policy)
                        .build());
        CredentialModel credential = provider.getCredentials();
        return CredentialModel.builder()
                .accessKeyId(credential.getAccessKeyId())
                .accessKeySecret(credential.getAccessKeySecret())
                .securityToken(credential.getSecurityToken())
                .type(credential.getType())
                .providerName(String.format("%s/%s", this.getProviderName(), credential.getProviderName()))
                .build();
    }

    private CredentialModel getSTSOIDCRoleSessionCredentials(Map<String, String> clientConfig,
                                                             CredentialsProviderFactory factory) {
        String roleSessionName = clientConfig.get(AuthConstant.INI_ROLE_SESSION_NAME);
        String roleArn = clientConfig.get(AuthConstant.INI_ROLE_ARN);
        String OIDCProviderArn = clientConfig.get(AuthConstant.INI_OIDC_PROVIDER_ARN);
        String OIDCTokenFilePath = clientConfig.get(AuthConstant.INI_OIDC_TOKEN_FILE_PATH);
        String regionId = clientConfig.get(AuthConstant.DEFAULT_REGION);
        String policy = clientConfig.get(AuthConstant.INI_POLICY);
        if (StringUtils.isEmpty(roleArn)) {
            throw new CredentialException("The configured role_arn is empty.");
        }
        if (StringUtils.isEmpty(OIDCProviderArn)) {
            throw new CredentialException("The configured oidc_provider_arn is empty.");
        }
        OIDCRoleArnCredentialProvider provider = factory.createCredentialsProvider(
                OIDCRoleArnCredentialProvider.builder()
                        .roleArn(roleArn)
                        .roleSessionName(roleSessionName)
                        .oidcProviderArn(OIDCProviderArn)
                        .oidcTokenFilePath(OIDCTokenFilePath)
                        .regionId(regionId)
                        .policy(policy)
                        .build());
        CredentialModel credential = provider.getCredentials();
        return CredentialModel.builder()
                .accessKeyId(credential.getAccessKeyId())
                .accessKeySecret(credential.getAccessKeySecret())
                .securityToken(credential.getSecurityToken())
                .type(credential.getType())
                .providerName(String.format("%s/%s", this.getProviderName(), credential.getProviderName()))
                .build();
    }

    private CredentialModel getSTSGetSessionAccessKeyCredentials(Map<String, String> clientConfig,
                                                                 CredentialsProviderFactory factory) {
        String publicKeyId = clientConfig.get(AuthConstant.INI_PUBLIC_KEY_ID);
        String privateKeyFile = clientConfig.get(AuthConstant.INI_PRIVATE_KEY_FILE);
        if (StringUtils.isEmpty(privateKeyFile)) {
            throw new CredentialException("The configured private_key_file is empty.");
        }
        String privateKey = AuthUtils.getPrivateKey(privateKeyFile);
        if (StringUtils.isEmpty(publicKeyId) || StringUtils.isEmpty(privateKey)) {
            throw new CredentialException("The configured public_key_id or private_key_file content is empty.");
        }
        RsaKeyPairCredentialProvider provider = factory.createCredentialsProvider(
                RsaKeyPairCredentialProvider.builder()
                        .publicKeyId(publicKeyId)
                        .privateKey(privateKey)
                        .build());
        CredentialModel credential = provider.getCredentials();
        return CredentialModel.builder()
                .accessKeyId(credential.getAccessKeyId())
                .accessKeySecret(credential.getAccessKeySecret())
                .securityToken(credential.getSecurityToken())
                .type(credential.getType())
                .providerName(String.format("%s/%s", this.getProviderName(), credential.getProviderName()))
                .build();
    }

    private CredentialModel getInstanceProfileCredentials(Map<String, String> clientConfig,
                                                          CredentialsProviderFactory factory) {
        String roleName = clientConfig.get(AuthConstant.INI_ROLE_NAME);
        if (StringUtils.isEmpty(roleName)) {
            throw new CredentialException("The configured role_name is empty.");
        }
        EcsRamRoleCredentialProvider provider = factory.createCredentialsProvider(
                EcsRamRoleCredentialProvider.builder()
                        .roleName(roleName)
                        .build());
        CredentialModel credential = provider.getCredentials();
        return CredentialModel.builder()
                .accessKeyId(credential.getAccessKeyId())
                .accessKeySecret(credential.getAccessKeySecret())
                .securityToken(credential.getSecurityToken())
                .type(credential.getType())
                .providerName(String.format("%s/%s", this.getProviderName(), credential.getProviderName()))
                .build();
    }

    @Override
    public String getProviderName() {
        return ProviderName.PROFILE;
    }

    @Override
    public void close() {
    }
}
