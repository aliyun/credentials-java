package com.aliyun.credentials.provider;


import com.aliyun.credentials.AccessKeyCredential;
import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.utils.StringUtils;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile;
import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class ProfileCredentialsProvider implements AlibabaCloudCredentialsProvider {
    private static volatile Wini ini;

    private static Wini getIni(String filePath) throws IOException {
        if (null == ini) {
            ini = new Wini(new File(filePath));
        }
        return ini;
    }

    @Override
    public AlibabaCloudCredentials getCredentials() {
        String filePath = AuthUtils.getEnvironmentCredentialsFile();
        if (filePath == null) {
            filePath = AuthConstant.DEFAULT_CREDENTIALS_FILE_PATH;
        }
        if (filePath.length() == 0) {
            throw new CredentialException("The specified credentials file is empty");
        }
        Wini ini;
        try {
            ini = getIni(filePath);
        } catch (IOException e) {
            return null;
        }
        Map<String, Map<String, String>> client = loadIni(ini);
        Map<String, String> clientConfig = client.get(AuthUtils.getClientType());
        if (clientConfig == null) {
            throw new CredentialException("Client is not open in the specified credentials file");
        }
        CredentialsProviderFactory credentialsProviderFactory = new CredentialsProviderFactory();
        return createCredential(clientConfig, credentialsProviderFactory);
    }

    private Map<String, Map<String, String>> loadIni(Wini ini) {
        Map<String, Map<String, String>> client = new HashMap<String, Map<String, String>>(16);
        boolean enable;
        for (Map.Entry<String, Profile.Section> clientType : ini.entrySet()) {
            enable = clientType.getValue().get(AuthConstant.INI_ENABLE, boolean.class);
            if (enable) {
                Map<String, String> clientConfig = new HashMap<String, String>(16);
                for (Map.Entry<String, String> enabledClient : clientType.getValue().entrySet()) {
                    clientConfig.put(enabledClient.getKey(), enabledClient.getValue());
                }
                client.put(clientType.getKey(), clientConfig);
            }
        }
        return client;
    }

    private AlibabaCloudCredentials createCredential(Map<String, String> clientConfig,
                                                     CredentialsProviderFactory factory) {
        String configType = clientConfig.get(AuthConstant.INI_TYPE);
        if (StringUtils.isEmpty(configType)) {
            throw new CredentialException("The configured client type is empty");
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
            return null;
        }
        return new AccessKeyCredential(accessKeyId, accessKeySecret);
    }

    private AlibabaCloudCredentials getSTSAssumeRoleSessionCredentials(Map<String, String> clientConfig,
                                                                       CredentialsProviderFactory factory) {
        String accessKeyId = clientConfig.get(AuthConstant.INI_ACCESS_KEY_ID);
        String accessKeySecret = clientConfig.get(AuthConstant.INI_ACCESS_KEY_IDSECRET);
        String roleSessionName = clientConfig.get(AuthConstant.INI_ROLE_SESSION_NAME);
        String roleArn = clientConfig.get(AuthConstant.INI_ROLE_ARN);
        String regionId = clientConfig.get(AuthConstant.DEFAULT_REGION);
        String policy = clientConfig.get(AuthConstant.INI_POLICY);
        if (StringUtils.isEmpty(accessKeyId) || StringUtils.isEmpty(accessKeySecret)) {
            throw new CredentialException("The configured access_key_id or access_key_secret is empty");
        }
        if (StringUtils.isEmpty(roleSessionName) || StringUtils.isEmpty(roleArn)) {
            throw new CredentialException("The configured role_session_name or role_arn is empty");
        }
        RamRoleArnCredentialProvider provider =
                factory.createCredentialsProvider(new RamRoleArnCredentialProvider(accessKeyId,
                        accessKeySecret, roleSessionName, roleArn, regionId, policy));
        return provider.getCredentials();
    }

    private AlibabaCloudCredentials getSTSOIDCRoleSessionCredentials(Map<String, String> clientConfig,
                                                                     CredentialsProviderFactory factory) {
        String roleSessionName = clientConfig.get(AuthConstant.INI_ROLE_SESSION_NAME);
        String roleArn = clientConfig.get(AuthConstant.INI_ROLE_ARN);
        String OIDCProviderArn = clientConfig.get(AuthConstant.INI_OIDC_PROVIDER_ARN);
        String OIDCTokenFilePath = clientConfig.get(AuthConstant.INI_OIDC_TOKEN_FILE_PATH);
        String regionId = clientConfig.get(AuthConstant.DEFAULT_REGION);
        String policy = clientConfig.get(AuthConstant.INI_POLICY);
        if (StringUtils.isEmpty(roleArn)) {
            throw new CredentialException("The configured role_arn is empty");
        }
        if (StringUtils.isEmpty(OIDCProviderArn)) {
            throw new CredentialException("The configured oidc_provider_arn is empty");
        }
        OIDCRoleArnCredentialProvider provider =
                factory.createCredentialsProvider(new OIDCRoleArnCredentialProvider(roleSessionName, roleArn, OIDCProviderArn, OIDCTokenFilePath, regionId, policy));
        return provider.getCredentials();
    }

    public AlibabaCloudCredentials getSTSGetSessionAccessKeyCredentials(Map<String, String> clientConfig,
                                                                        CredentialsProviderFactory factory) {
        String publicKeyId = clientConfig.get(AuthConstant.INI_PUBLIC_KEY_ID);
        String privateKeyFile = clientConfig.get(AuthConstant.INI_PRIVATE_KEY_FILE);
        if (StringUtils.isEmpty(privateKeyFile)) {
            throw new CredentialException("The configured private_key_file is empty");
        }
        String privateKey = AuthUtils.getPrivateKey(privateKeyFile);
        if (StringUtils.isEmpty(publicKeyId) || StringUtils.isEmpty(privateKey)) {
            throw new CredentialException("The configured public_key_id or private_key_file content is empty");
        }
        RsaKeyPairCredentialProvider provider =
                factory.createCredentialsProvider(new RsaKeyPairCredentialProvider(publicKeyId, privateKey));
        return provider.getCredentials();
    }

    private AlibabaCloudCredentials getInstanceProfileCredentials(Map<String, String> clientConfig,
                                                                  CredentialsProviderFactory factory) {
        String roleName = clientConfig.get(AuthConstant.INI_ROLE_NAME);
        if (StringUtils.isEmpty(roleName)) {
            throw new CredentialException("The configured role_name is empty");
        }
        EcsRamRoleCredentialProvider provider =
                factory.createCredentialsProvider(new EcsRamRoleCredentialProvider(roleName));
        return provider.getCredentials();
    }
}
