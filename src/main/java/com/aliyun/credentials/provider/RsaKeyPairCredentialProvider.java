package com.aliyun.credentials.provider;

import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.RsaKeyPairCredential;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.http.MethodType;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.utils.ParameterHelper;
import com.aliyun.credentials.utils.StringUtils;
import com.google.gson.Gson;

import java.util.Map;


public class RsaKeyPairCredentialProvider implements AlibabaCloudCredentialsProvider {

    /**
     * Default duration for started sessions. Unit of Second
     */
    public int durationSeconds = 3600;

    private String publicKeyId;
    private String privateKey;
    private String regionId = "cn-hangzhou";

    /**
     * Unit of millisecond
     */
    private int connectTimeout = 1000;
    private int readTimeout = 1000;

    /**
     * Endpoint of RAM OpenAPI
     */
    private String STSEndpoint = "sts.aliyuncs.com";

    public RsaKeyPairCredentialProvider(Configuration config) {
        this(config.getPublicKeyId(), config.getPrivateKeyFile());
        this.connectTimeout = config.getConnectTimeout();
        this.readTimeout = config.getReadTimeout();
        if (!StringUtils.isEmpty(config.getSTSEndpoint())) {
            this.STSEndpoint = config.getSTSEndpoint();
        }
    }

    public RsaKeyPairCredentialProvider(Config config) {
        this(config.publicKeyId, config.privateKeyFile);
        this.connectTimeout = config.connectTimeout;
        this.readTimeout = config.timeout;
        if (!StringUtils.isEmpty(config.STSEndpoint)) {
            this.STSEndpoint = config.STSEndpoint;
        }
    }


    public RsaKeyPairCredentialProvider(String publicKeyId, String privateKey) {
        this.publicKeyId = publicKeyId;
        this.privateKey = privateKey;
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
        httpRequest.setUrlParameter("Action", "GenerateSessionAccessKey");
        httpRequest.setUrlParameter("Format", "JSON");
        httpRequest.setUrlParameter("Version", "2015-04-01");
        httpRequest.setUrlParameter("DurationSeconds", String.valueOf(durationSeconds));
        httpRequest.setUrlParameter("AccessKeyId", this.publicKeyId);
        httpRequest.setUrlParameter("RegionId", this.regionId);
        String strToSign = parameterHelper.composeStringToSign(MethodType.GET, httpRequest.getUrlParameters());
        String signature = parameterHelper.signString(strToSign, this.privateKey + "&");
        httpRequest.setUrlParameter("Signature", signature);
        httpRequest.setSysMethod(MethodType.GET);
        httpRequest.setSysConnectTimeout(this.connectTimeout);
        httpRequest.setSysReadTimeout(this.readTimeout);
        httpRequest.setSysUrl(parameterHelper.composeUrl(this.STSEndpoint, httpRequest.getUrlParameters(),
                "https"));
        HttpResponse httpResponse = client.syncInvoke(httpRequest);
        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(httpResponse.getHttpContentString(), Map.class);
        Map<String, String> credential = (Map<String, String>) map.get("SessionAccessKey");
        long expiration = ParameterHelper.getUTCDate(credential.get("Expiration")).getTime();
        return new RsaKeyPairCredential(credential.get("SessionAccessKeyId"), credential.get("SessionAccessKeySecret"),
                expiration, this);

    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public void setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
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

    public String getSTSEndpoint() {
        return STSEndpoint;
    }

    public void setSTSEndpoint(String STSEndpoint) {
        this.STSEndpoint = STSEndpoint;
    }
}
