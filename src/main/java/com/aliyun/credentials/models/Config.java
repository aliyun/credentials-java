package com.aliyun.credentials.models;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.tea.NameInMap;
import com.aliyun.tea.TeaModel;

public class Config extends TeaModel {
    @NameInMap("type")
    public String type = "default";
    @NameInMap("accessKeyId")
    public String accessKeyId;
    @NameInMap("accessKeySecret")
    public String accessKeySecret;
    @NameInMap("roleArn")
    public String roleArn;
    @NameInMap("roleSessionName")
    public String roleSessionName;
    @NameInMap("privateKeyFile")
    public String privateKeyFile;
    @NameInMap("publicKeyId")
    public String publicKeyId;
    @NameInMap("roleName")
    public String roleName;
    @NameInMap("enableIMDSv2")
    public Boolean enableIMDSv2 = false;
    @NameInMap("metadataTokenDuration")
    public Integer metadataTokenDuration = 21600;
    @NameInMap("bearerToken")
    public String bearerToken;
    @NameInMap("securityToken")
    public String securityToken;
    @NameInMap("host")
    public String host;
    @NameInMap("readTimeout")
    public int timeout = 10000;
    @NameInMap("connectTimeout")
    public int connectTimeout = 5000;
    @NameInMap("proxy")
    public String proxy;
    @NameInMap("policy")
    public String policy;
    @NameInMap("roleSessionExpiration")
    public Integer roleSessionExpiration = 3600;
    @NameInMap("oidcProviderArn")
    public String oidcProviderArn;
    @NameInMap("oidcTokenFilePath")
    public String oidcTokenFilePath;
    @NameInMap("credentialsURI")
    public String credentialsURI;
    @NameInMap("STSEndpoint")
    public String STSEndpoint = "sts.aliyuncs.com";
    /**
     * <p>external id for ram role arn</p>
     */
    @NameInMap("externalId")
    public String externalId;

    public static Config build(java.util.Map<String, ?> map) {
        Config self = new Config();
        try {
            return TeaModel.build(map, self);
        } catch (Exception e) {
            throw new CredentialException(e.getMessage(), e);
        }
    }

    public Config setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
        return this;
    }

    public String getAccessKeyId() {
        return this.accessKeyId;
    }

    public Config setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
        return this;
    }

    public String getAccessKeySecret() {
        return this.accessKeySecret;
    }

    public Config setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
        return this;
    }

    public String getSecurityToken() {
        return this.securityToken;
    }

    public Config setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
        return this;
    }

    public String getBearerToken() {
        return this.bearerToken;
    }

    public Config setRoleArn(String roleArn) {
        this.roleArn = roleArn;
        return this;
    }

    public String getRoleArn() {
        return this.roleArn;
    }

    public Config setPolicy(String policy) {
        this.policy = policy;
        return this;
    }

    public String getPolicy() {
        return this.policy;
    }

    public Config setRoleSessionExpiration(Integer roleSessionExpiration) {
        this.roleSessionExpiration = roleSessionExpiration;
        return this;
    }

    public Number getRoleSessionExpiration() {
        return this.roleSessionExpiration;
    }

    public Config setRoleSessionName(String roleSessionName) {
        this.roleSessionName = roleSessionName;
        return this;
    }

    public String getRoleSessionName() {
        return this.roleSessionName;
    }

    public Config setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
        return this;
    }

    public String getPublicKeyId() {
        return this.publicKeyId;
    }

    public Config setPrivateKeyFile(String privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
        return this;
    }

    public String getPrivateKeyFile() {
        return this.privateKeyFile;
    }

    public Config setRoleName(String roleName) {
        this.roleName = roleName;
        return this;
    }

    public String getRoleName() {
        return this.roleName;
    }


    public Config setEnableIMDSv2(Boolean enableIMDSv2) {
        this.enableIMDSv2 = enableIMDSv2;
        return this;
    }

    public Boolean getEnableIMDSv2() {
        return this.enableIMDSv2;
    }

    public Config setMetadataTokenDuration(Integer metadataTokenDuration) {
        this.metadataTokenDuration = metadataTokenDuration;
        return this;
    }

    public Integer getMetadataTokenDuration() {
        return this.metadataTokenDuration;
    }

    public Config setCredentialsUri(String credentialsURI) {
        this.credentialsURI = credentialsURI;
        return this;
    }

    public String getCredentialsUri() {
        return this.credentialsURI;
    }

    public Config setType(String type) {
        this.type = type;
        return this;
    }

    public String getType() {
        return this.type;
    }

    public Config setSTSEndpoint(String STSEndpoint) {
        this.STSEndpoint = STSEndpoint;
        return this;
    }

    public String getSTSEndpoint() {
        return this.STSEndpoint;
    }

    public Config setExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public String getExternalId() {
        return this.externalId;
    }

    public Config setHost(String host) {
        this.host = host;
        return this;
    }

    public String getHost() {
        return host;
    }

    public Config setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public Config setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public Config setProxy(String proxy) {
        this.proxy = proxy;
        return this;
    }

    public String getProxy() {
        return proxy;
    }

    public Config setOidcProviderArn(String oidcProviderArn) {
        this.oidcProviderArn = oidcProviderArn;
        return this;
    }

    public String getOidcProviderArn() {
        return oidcProviderArn;
    }

    public Config setOidcTokenFilePath(String oidcTokenFilePath) {
        this.oidcTokenFilePath = oidcTokenFilePath;
        return this;
    }

    public String getOidcTokenFilePath() {
        return oidcTokenFilePath;
    }
}
