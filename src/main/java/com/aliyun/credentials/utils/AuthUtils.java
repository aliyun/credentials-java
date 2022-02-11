package com.aliyun.credentials.utils;

import com.aliyun.credentials.exception.CredentialException;

import java.io.*;


public class AuthUtils {
    private static volatile String clientType = System.getenv("ALIBABA_CLOUD_PROFILE");
    private static volatile String environmentAccessKeyId;
    private static volatile String environmentAccesskeySecret;
    private static volatile String environmentECSMetaData;
    private static volatile String environmentCredentialsFile;
    private static volatile String privateKey;
    private static volatile String OIDCToken;

    public static String getPrivateKey(String filePath) {
        FileInputStream in = null;
        byte[] buffer;
        try {
            in = new FileInputStream(new File(filePath));
            buffer = new byte[in.available()];
            in.read(buffer);
            privateKey = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
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
        if(!file.canRead()){
            throw new CredentialException("OIDCTokenFilePath " + OIDCTokenFilePath + " cannot be read.");
        }
        try {
            in = new FileInputStream(file);
            buffer = new byte[in.available()];
            in.read(buffer);
            OIDCToken = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
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
        if (null == AuthUtils.environmentAccessKeyId) {
            return System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID");
        } else {
            return AuthUtils.environmentAccessKeyId;
        }
    }


    public static String getEnvironmentAccessKeySecret() {
        if (null == AuthUtils.environmentAccesskeySecret) {
            return System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET");
        } else {
            return AuthUtils.environmentAccesskeySecret;
        }
    }

    public static void setEnvironmentAccessKeySecret(String environmentAccesskeySecret) {
        AuthUtils.environmentAccesskeySecret = environmentAccesskeySecret;
    }

    public static void setEnvironmentECSMetaData(String environmentECSMetaData) {
        AuthUtils.environmentECSMetaData = environmentECSMetaData;
    }

    public static String getEnvironmentECSMetaData() {
        if (null == AuthUtils.environmentECSMetaData) {
            return System.getenv("ALIBABA_CLOUD_ECS_METADATA");
        } else {
            return AuthUtils.environmentECSMetaData;
        }
    }

    public static String getEnvironmentCredentialsFile() {
        if (null == AuthUtils.environmentCredentialsFile) {
            return System.getenv("ALIBABA_CLOUD_CREDENTIALS_FILE");
        } else {
            return AuthUtils.environmentCredentialsFile;
        }
    }

    public static void setEnvironmentCredentialsFile(String environmentCredentialsFile) {
        AuthUtils.environmentCredentialsFile = environmentCredentialsFile;
    }

}
