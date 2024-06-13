package com.aliyun.credentials.utils;

import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.provider.AlibabaCloudCredentialsProvider;

import java.util.Date;

@Deprecated
public class RefreshUtils {

    public static boolean withShouldRefresh(long expiration) {
        return new Date().getTime() >= (expiration - 180 * 1000);
    }

    public static AlibabaCloudCredentials getNewCredential(AlibabaCloudCredentialsProvider provider) {
        try {
            return provider.getCredentials();
        } catch (Exception e) {
            throw new CredentialException(e.getMessage(), e);
        }
    }
}
