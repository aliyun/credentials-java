package com.aliyun.credentials.provider;

import java.util.Map;

import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.CredentialsURICredential;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.http.MethodType;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.models.Config;
import com.google.gson.Gson;

public class CredentialsURICredentialProvider implements AlibabaCloudCredentialsProvider {
    private int connectionTimeout = 1000;
    private int readTimeout = 1000;

    public CredentialsURICredentialProvider(Config config) {
        this(config.credentialsURI);
    }

    public CredentialsURICredentialProvider() {
    }

    private String credentialUrl;

    public CredentialsURICredentialProvider(String uri) {
        this.credentialUrl = uri;
    }

    @Override
    public AlibabaCloudCredentials getCredentials() {
        CompatibleUrlConnClient client = new CompatibleUrlConnClient();
        String jsonContent = getMetadata(client);
        Map<String, String> result = new Gson().fromJson(jsonContent, Map.class);

        if (!"Success".equals(result.get("Code"))) {
            throw new CredentialException("");
        }

        return new CredentialsURICredential(result.get("AccessKeyId"), result.get("AccessKeySecret"),
                result.get("SecurityToken"), result.get("Expiration"), this);
    }

    private String getMetadata(CompatibleUrlConnClient client) {
        HttpRequest request = new HttpRequest(credentialUrl.toString());
        request.setSysMethod(MethodType.GET);
        request.setSysConnectTimeout(connectionTimeout);
        request.setSysReadTimeout(readTimeout);
        HttpResponse response;

        try {
            response = client.syncInvoke(request);
        } catch (Exception e) {
            throw new CredentialException("Fetch credentials failed. " + e.toString());
        } finally {
            client.close();
        }

        if (response.getResponseCode() != 200) {
            throw new CredentialException("Fetch credentials failed, HttpCode=" + response.getResponseCode());
        }

        return new String(response.getHttpContent());
    }
}
