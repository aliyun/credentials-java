package com.aliyun.credentials;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.provider.*;
import com.aliyun.credentials.utils.AuthConstant;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;

public class Credential {

    private AlibabaCloudCredentials cloudCredential;

    public Credential(Configuration config) throws ParseException, CredentialException, IOException {
        this.cloudCredential = getCredential(config);
    }

    public AlibabaCloudCredentials getCredential(Configuration config) throws IOException, CredentialException, ParseException {
        switch (config.getType()) {
            case AuthConstant.ACCESS_KEY:
                return new AccessKeyCredential(config.getAccessKeyId(), config.getAccessKeySecret());
            case AuthConstant.STS:
                return new StsCredential(config.getAccessKeyId(), config.getAccessKeySecret(), config.getSecurityToken());
            default:
                return this.getProvider(config).getCredentials();
        }
    }

    private AlibabaCloudCredentialsProvider getProvider(Configuration config) throws CredentialException, MalformedURLException {
        try {
            switch (config.getType()) {
                case AuthConstant.ECS_RAM_ROLE:
                    return new EcsRamRoleCredentialProvider(config);
                case AuthConstant.RAM_ROLE_ARN:
                    return new RamRoleArnCredentialProvider(config);
                case AuthConstant.RSA_KEY_PAIR:
                    return new RsaKeyPairCredentialProvider(config);
                default:
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DefaultCredentialsProvider();
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
