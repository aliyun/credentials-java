package com.aliyun.credentials.utils;

import com.aliyun.credentials.exception.CredentialException;

import java.io.*;

public class AuthUtils {
    private static volatile String clientType = System.getenv("ALIBABA_CLOUD_PROFILE");
    private static volatile String environmentAccessKeyId;
    private static volatile String environmentAccesskeySecret;
    private static volatile String environmentSecurityToken;
    private static volatile String environmentECSMetaData;
    private static volatile Boolean enableECSIMDSv2;
    private static volatile Boolean disableECSIMDSv1;
    private static volatile String environmentCredentialsFile;
    private static volatile String environmentRoleArn;
    private static volatile String environmentOIDCProviderArn;
    private static volatile String environmentOIDCTokenFilePath;
    private static volatile String privateKey;
    private static volatile String OIDCToken;
    private static volatile Boolean disableCLIProfile;
    private static volatile String environmentCredentialsURI;

    public static String getPrivateKey(String filePath) {
        FileInputStream in = null;
        byte[] buffer;
        try {
            in = new FileInputStream(new File(filePath));
            buffer = new byte[in.available()];
            in.read(buffer);
            privateKey = new String(buffer, "UTF-8");
        } catch (IOException e) {
            throw new CredentialException(e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new CredentialException(e.getMessage(), e);
                }
            }
        }
        return privateKey;
    }

    public static String getOIDCToken(String OIDCTokenFilePath) {
        FileInputStream in = null;
        byte[] buffer;
        File file = new File(OIDCTokenFilePath);
        if (!file.exists() || !file.isFile()) {
            throw new CredentialException("OIDCTokenFilePath " + OIDCTokenFilePath + " is not exists.");
        }
        if (!file.canRead()) {
            throw new CredentialException("OIDCTokenFilePath " + OIDCTokenFilePath + " cannot be read.");
        }
        try {
            in = new FileInputStream(file);
            buffer = new byte[in.available()];
            in.read(buffer);
            OIDCToken = new String(buffer, "UTF-8");
        } catch (IOException e) {
            throw new CredentialException(e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new CredentialException(e.getMessage(), e);
                }
            }
        }
        return OIDCToken;
    }

    public static void setClientType(String clientType) {
        AuthUtils.clientType = clientType;
    }

    public static void setPrivateKey(String key) {
        privateKey = key;
    }

    public static String getClientType() {
        if (null == clientType) {
            AuthUtils.clientType = "default";
            return AuthUtils.clientType;
        } else {
            return AuthUtils.clientType;
        }
    }

    public static void setEnvironmentAccessKeyId(String environmentAccessKeyId) {
        AuthUtils.environmentAccessKeyId = environmentAccessKeyId;
    }

    public static String getEnvironmentAccessKeyId() {
        return null == AuthUtils.environmentAccessKeyId ?
                System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID")
                : AuthUtils.environmentAccessKeyId;
    }


    public static String getEnvironmentAccessKeySecret() {
        return null == AuthUtils.environmentAccesskeySecret ?
                System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET")
                : AuthUtils.environmentAccesskeySecret;
    }

    public static void setEnvironmentAccessKeySecret(String environmentAccesskeySecret) {
        AuthUtils.environmentAccesskeySecret = environmentAccesskeySecret;
    }

    public static String getEnvironmentSecurityToken() {
        return null == AuthUtils.environmentSecurityToken ?
                System.getenv("ALIBABA_CLOUD_SECURITY_TOKEN")
                : AuthUtils.environmentSecurityToken;
    }

    public static void setEnvironmentSecurityToken(String environmentSecurityToken) {
        AuthUtils.environmentSecurityToken = environmentSecurityToken;
    }

    public static void setEnvironmentECSMetaData(String environmentECSMetaData) {
        AuthUtils.environmentECSMetaData = environmentECSMetaData;
    }

    public static String getEnvironmentECSMetaData() {
        return null == AuthUtils.environmentECSMetaData ?
                System.getenv("ALIBABA_CLOUD_ECS_METADATA")
                : AuthUtils.environmentECSMetaData;
    }

    public static void enableECSIMDSv2(boolean enableECSIMDSv2) {
        AuthUtils.enableECSIMDSv2 = enableECSIMDSv2;
    }

    public static boolean getEnableECSIMDSv2() {
        if (null != AuthUtils.enableECSIMDSv2) {
            return AuthUtils.enableECSIMDSv2;
        } else if (null != System.getenv("ALIBABA_CLOUD_ECS_IMDSV2_ENABLE")) {
            return Boolean.parseBoolean(System.getenv("ALIBABA_CLOUD_ECS_IMDSV2_ENABLE"));
        }
        return false;
    }

    public static void disableECSIMDSv1(boolean disableECSIMDSv1) {
        AuthUtils.disableECSIMDSv1 = disableECSIMDSv1;
    }

    public static boolean getDisableECSIMDSv1() {
        if (null != AuthUtils.disableECSIMDSv1) {
            return AuthUtils.disableECSIMDSv1;
        } else if (null != System.getenv("ALIBABA_CLOUD_IMDSV1_DISABLED")) {
            return Boolean.parseBoolean(System.getenv("ALIBABA_CLOUD_IMDSV1_DISABLED"));
        }
        return false;
    }

    public static void setEnvironmentRoleArn(String environmentRoleArn) {
        AuthUtils.environmentRoleArn = environmentRoleArn;
    }

    public static String getEnvironmentRoleArn() {
        return null == AuthUtils.environmentRoleArn ?
                System.getenv("ALIBABA_CLOUD_ROLE_ARN")
                : AuthUtils.environmentRoleArn;
    }

    public static void setEnvironmentOIDCProviderArn(String environmentOIDCProviderArn) {
        AuthUtils.environmentOIDCProviderArn = environmentOIDCProviderArn;
    }

    public static String getEnvironmentOIDCProviderArn() {
        return null == AuthUtils.environmentOIDCProviderArn ?
                System.getenv("ALIBABA_CLOUD_OIDC_PROVIDER_ARN")
                : AuthUtils.environmentOIDCProviderArn;
    }

    public static void setEnvironmentOIDCTokenFilePath(String environmentOIDCTokenFilePath) {
        AuthUtils.environmentOIDCTokenFilePath = environmentOIDCTokenFilePath;
    }

    public static String getEnvironmentOIDCTokenFilePath() {
        return null == AuthUtils.environmentOIDCTokenFilePath ?
                System.getenv("ALIBABA_CLOUD_OIDC_TOKEN_FILE")
                : AuthUtils.environmentOIDCTokenFilePath;
    }

    public static boolean environmentEnableOIDC() {
        return !StringUtils.isEmpty(getEnvironmentRoleArn())
                && !StringUtils.isEmpty(getEnvironmentOIDCProviderArn())
                && !StringUtils.isEmpty(getEnvironmentOIDCTokenFilePath());
    }

    public static String getEnvironmentCredentialsFile() {
        return null == AuthUtils.environmentCredentialsFile ?
                System.getenv("ALIBABA_CLOUD_CREDENTIALS_FILE")
                : AuthUtils.environmentCredentialsFile;
    }

    public static void setEnvironmentCredentialsFile(String environmentCredentialsFile) {
        AuthUtils.environmentCredentialsFile = environmentCredentialsFile;
    }

    public static void disableCLIProfile(boolean disableCLIProfile) {
        AuthUtils.disableCLIProfile = disableCLIProfile;
    }

    public static boolean isDisableCLIProfile() {
        if (null != AuthUtils.disableCLIProfile) {
            return AuthUtils.disableCLIProfile;
        } else if (null != System.getenv("ALIBABA_CLOUD_CLI_PROFILE_DISABLED")) {
            return Boolean.parseBoolean(System.getenv("ALIBABA_CLOUD_CLI_PROFILE_DISABLED"));
        }
        return false;
    }

    public static void setEnvironmentCredentialsURI(String environmentCredentialsURI) {
        AuthUtils.environmentCredentialsURI = environmentCredentialsURI;
    }

    public static String getEnvironmentCredentialsURI() {
        return null == AuthUtils.environmentCredentialsURI ?
                System.getenv("ALIBABA_CLOUD_CREDENTIALS_URI")
                : AuthUtils.environmentCredentialsURI;
    }

}
