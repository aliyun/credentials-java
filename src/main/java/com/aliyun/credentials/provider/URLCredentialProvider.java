package com.aliyun.credentials.provider;

import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.URLCredential;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.http.MethodType;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.utils.ParameterHelper;
import com.aliyun.credentials.utils.StringUtils;
import com.google.gson.Gson;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class URLCredentialProvider implements AlibabaCloudCredentialsProvider {

    private URL credentialsURI;
    /**
     * Unit of millisecond
     */
    private int connectTimeout = 1000;
    private int readTimeout = 1000;

    public URLCredentialProvider(String credentialsURI) {
        if (StringUtils.isEmpty(credentialsURI)) {
            throw new CredentialException("URL cannot be null.");
        }
        try {
            this.credentialsURI = new URL(credentialsURI);
        } catch (MalformedURLException e) {
            throw new CredentialException("URL is not valid.");
        }
    }

    public URLCredentialProvider(URL credentialsURI) {
        if (credentialsURI == null) {
            throw new CredentialException("URL cannot be null.");
        }
        this.credentialsURI = credentialsURI;
    }

    public URLCredentialProvider(Configuration config) {
        this(config.getCredentialsURI());
        this.connectTimeout = config.getConnectTimeout();
        this.readTimeout = config.getReadTimeout();
    }

    public URLCredentialProvider(Config config) {
        this(config.credentialsURI);
        this.connectTimeout = config.connectTimeout;
        this.readTimeout = config.timeout;
    }

    @Override
    public AlibabaCloudCredentials getCredentials() {
        CompatibleUrlConnClient client = new CompatibleUrlConnClient();
        HttpRequest request = new HttpRequest(this.credentialsURI.toString());
        request.setSysMethod(MethodType.GET);
        request.setSysConnectTimeout(connectTimeout);
        request.setSysReadTimeout(readTimeout);
        HttpResponse response;

        try {
            response = client.syncInvoke(request);
        } catch (Exception e) {
            throw new CredentialException("Failed to connect Server: " + e.toString());
        } finally {
            client.close();
        }

        if (response.getResponseCode() >= 300 || response.getResponseCode() < 200) {
            throw new CredentialException("Failed to get credentials from server: " + this.credentialsURI.toString()
                    + "\nHttpCode=" + response.getResponseCode()
                    + "\nHttpRAWContent=" + response.getHttpContentString());
        }

        Gson gson = new Gson();
        Map<String, String> map;
        try {
            map = gson.fromJson(response.getHttpContentString(), Map.class);
        } catch (Exception e) {
            throw new CredentialException("Failed to get credentials from server: " + this.credentialsURI.toString()
                    + "\nHttpCode=" + response.getResponseCode()
                    + "\nHttpRAWContent=" + response.getHttpContentString(), e);
        }
        if (map.containsKey("Code") && map.get("Code").equals("Success")) {
            long expiration = ParameterHelper.getUTCDate(map.get("Expiration")).getTime();
            return new URLCredential(map.get("AccessKeyId"), map.get("AccessKeySecret"),
                    map.get("SecurityToken"), expiration, this);
        } else {
            throw new CredentialException(gson.toJson(map));
        }
    }

    public String getURL() {
        return credentialsURI.toString();
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