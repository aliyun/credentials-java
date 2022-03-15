package com.aliyun.credentials.provider;


import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.RamRoleArnCredential;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.http.MethodType;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.utils.ParameterHelper;
import com.google.gson.Gson;

import java.util.Map;

public class RamRoleArnCredentialProvider implements AlibabaCloudCredentialsProvider {

    /**
     * Default duration for started sessions. Unit of Second
     */
    public int durationSeconds = 3600;
    /**
     * The arn of the role to be assumed.
     */
    private String roleArn;
    /**
     * An identifier for the assumed role session.
     */
    private String roleSessionName = "defaultSessionName";

    private String accessKeyId;
    private String accessKeySecret;
    private String regionId = "cn-hangzhou";
    private String policy;

    /**
     * Unit of millisecond
     */
    private int connectTimeout = 1000;
    private int readTimeout = 1000;

    public RamRoleArnCredentialProvider(Configuration config) {
        this(config.getAccessKeyId(), config.getAccessKeySecret(), config.getRoleArn());
        this.roleSessionName = config.getRoleSessionName();
        this.connectTimeout = config.getConnectTimeout();
        this.readTimeout = config.getReadTimeout();
    }

    public RamRoleArnCredentialProvider(Config config) {
        this(config.accessKeyId, config.accessKeySecret, config.roleArn);
        this.roleSessionName = config.roleSessionName;
        this.connectTimeout = config.connectTimeout;
        this.readTimeout = config.timeout;
        this.policy = config.policy;
        this.durationSeconds = config.roleSessionExpiration;
    }

    public RamRoleArnCredentialProvider(String accessKeyId, String accessKeySecret, String roleArn) {
        this.roleArn = roleArn;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
    }

    public RamRoleArnCredentialProvider(String accessKeyId, String accessKeySecret, String roleSessionName,
                                        String roleArn, String regionId, String policy) {
        this(accessKeyId, accessKeySecret, roleArn);
        this.roleSessionName = roleSessionName;
        this.regionId = regionId;
        this.policy = policy;
    }


    @Override
    public AlibabaCloudCredentials getCredentials() {
        CompatibleUrlConnClient client = new CompatibleUrlConnClient();
        return createCredential(client);
    }

    public AlibabaCloudCredentials createCredential(CompatibleUrlConnClient client) {
        try {
            return getNewSessionCredentials(client);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client.close();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public AlibabaCloudCredentials getNewSessionCredentials(CompatibleUrlConnClient client) {
        ParameterHelper parameterHelper = new ParameterHelper();
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setUrlParameter("Action", "AssumeRole");
        httpRequest.setUrlParameter("Format", "JSON");
        httpRequest.setUrlParameter("Version", "2015-04-01");
        httpRequest.setUrlParameter("DurationSeconds", String.valueOf(durationSeconds));
        httpRequest.setUrlParameter("RoleArn", this.roleArn);
        httpRequest.setUrlParameter("AccessKeyId", this.accessKeyId);
        httpRequest.setUrlParameter("RegionId", this.regionId);
        httpRequest.setUrlParameter("RoleSessionName", this.roleSessionName);
        if (policy != null) {
            httpRequest.setUrlParameter("Policy", this.policy);
        }
        httpRequest.setSysMethod(MethodType.GET);
        httpRequest.setSysConnectTimeout(this.connectTimeout);
        httpRequest.setSysReadTimeout(this.readTimeout);
        String strToSign = parameterHelper.composeStringToSign(MethodType.GET, httpRequest.getUrlParameters());
        String signature = parameterHelper.signString(strToSign, this.accessKeySecret + "&");
        httpRequest.setUrlParameter("Signature", signature);
        httpRequest.setSysUrl(parameterHelper.composeUrl("sts.aliyuncs.com", httpRequest.getUrlParameters(),
                "https"));
        HttpResponse httpResponse = client.syncInvoke(httpRequest);
        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(httpResponse.getHttpContentString(), Map.class);
        if (map.containsKey("Credentials")) {
            Map<String, String> credential = (Map<String, String>) map.get("Credentials");
            long expiration = ParameterHelper.getUTCDate(credential.get("Expiration")).getTime();
            return new RamRoleArnCredential(credential.get("AccessKeyId"), credential.get("AccessKeySecret"),
                    credential.get("SecurityToken"), expiration, this);
        } else {
            throw new CredentialException(gson.toJson(map));
        }
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public String getRoleSessionName() {
        return roleSessionName;
    }

    public void setRoleSessionName(String roleSessionName) {
        this.roleSessionName = roleSessionName;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}