package com.aliyun.credentials;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.provider.*;
import com.aliyun.credentials.utils.AuthConstant;

@Deprecated
public class Credential {

    private AlibabaCloudCredentials cloudCredential;

    public Credential(Configuration config) {
        if (null == config) {
            DefaultCredentialsProvider provider = new DefaultCredentialsProvider();
            this.cloudCredential = provider.getCredentials();
            return;
        }
        this.cloudCredential = getCredential(config);
    }

    public AlibabaCloudCredentials getCredential(Configuration config) {
        switch (config.getType()) {
            case AuthConstant.ACCESS_KEY:
                return new AccessKeyCredential(config.getAccessKeyId(), config.getAccessKeySecret());
            case AuthConstant.STS:
                return new StsCredential(config.getAccessKeyId(), config.getAccessKeySecret(), config.getSecurityToken());
            default:
                return this.getProvider(config).getCredentials();
        }
    }

    private AlibabaCloudCredentialsProvider getProvider(Configuration config) {
        switch (config.getType()) {
            case AuthConstant.ECS_RAM_ROLE:
                return new EcsRamRoleCredentialProvider(config);
            case AuthConstant.RAM_ROLE_ARN:
                return new RamRoleArnCredentialProvider(config);
            case AuthConstant.RSA_KEY_PAIR:
                return new RsaKeyPairCredentialProvider(config);
            case AuthConstant.OIDC_ROLE_ARN:
                return new OIDCRoleArnCredentialProvider(config);
            default:
                throw new CredentialException("invalid type option, support: access_key, sts, ecs_ram_role, ram_role_arn, rsa_key_pair");
        }
    }

    public String getAccessKeyId() {
        return this.cloudCredential.getAccessKeyId();
    }

    public String getAccessKeySecret() {
        return this.cloudCredential.getAccessKeySecret();
    }

    public String getSecurityToken() {
        return this.cloudCredential.getSecurityToken();
    }

    public String getType() {
        return this.cloudCredential.getType();
    }
}
