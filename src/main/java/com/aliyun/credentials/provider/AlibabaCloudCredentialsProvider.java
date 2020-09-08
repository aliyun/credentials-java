package com.aliyun.credentials.provider;

import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.exception.CredentialException;

import java.io.IOException;
import java.text.ParseException;

public interface AlibabaCloudCredentialsProvider {

    public AlibabaCloudCredentials getCredentials();
}
