package com.aliyun.credentials.utils;

import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.provider.AlibabaCloudCredentialsProvider;

public class RefreshUtils {

    public static boolean withShouldRefresh(long expiration) {
        return System.currentTimeMillis() >= (expiration - 180);
    }

    public static AlibabaCloudCredentials getNewCredential(AlibabaCloudCredentialsProvider provider) {
        try {
            return provider.getCredentials();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
