package com.aliyun.credentials;

public interface AlibabaCloudCredentials {

    public String getAccessKeyId();

    public String getAccessKeySecret();

    public String getSecurityToken();

    public String getType();
}
