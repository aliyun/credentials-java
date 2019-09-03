package com.aliyun.credentials.provider;


import com.aliyun.credentials.EcsRamRoleCredential;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.http.MethodType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

public class ECSMetadataServiceCredentialsFetcher {
    private static final String URL_IN_ECS_METADATA = "/latest/meta-data/ram/security-credentials/";
    private static final String ECS_METADAT_FETCH_ERROR_MSG = "Failed to get RAM session credentials from ECS metadata service.";
    private URL credentialUrl;
    private String roleName;
    private String metadataServiceHost = "100.100.100.200";
    private int connectionTimeout = 1000;
    private int readTimeout = 1000;


    public ECSMetadataServiceCredentialsFetcher(String roleName, int connectionTimeout, int readTimeout) throws MalformedURLException {
        if (connectionTimeout > 1000) {
            this.connectionTimeout = connectionTimeout;
        }
        if (readTimeout > 1000) {
            this.readTimeout = readTimeout;
        }
        this.roleName = roleName;
        setCredentialUrl();
    }

    public ECSMetadataServiceCredentialsFetcher(String roleName) throws MalformedURLException {
        this.roleName = roleName;
        setCredentialUrl();
    }


    private void setCredentialUrl() throws MalformedURLException {
        this.credentialUrl = new URL("http://" + metadataServiceHost + URL_IN_ECS_METADATA + roleName);
    }

    public String getMetadata(CompatibleUrlConnClient client) throws CredentialException {
        HttpRequest request = new HttpRequest(credentialUrl.toString());
        request.setSysMethod(MethodType.GET);
        request.setSysConnectTimeout(connectionTimeout);
        request.setSysReadTimeout(readTimeout);
        HttpResponse response;

        try {
            response = client.syncInvoke(request);
        } catch (Exception e) {
            throw new CredentialException("Failed to connect ECS Metadata Service: " + e.toString());
        } finally {
            client.close();
        }

        if (response.getResponseCode() != 200) {
            throw new CredentialException(ECS_METADAT_FETCH_ERROR_MSG + " HttpCode=" + response.getResponseCode());
        }

        return new String(response.getHttpContent());
    }

    public EcsRamRoleCredential fetch(CompatibleUrlConnClient client) throws CredentialException, ParseException {
        String jsonContent = getMetadata(client);
        JsonObject jsonObject = new JsonParser().parse(jsonContent).getAsJsonObject();

        if (jsonObject.has("Code") && jsonObject.has("AccessKeyId") && jsonObject.has("AccessKeySecret") && jsonObject
                .has("SecurityToken") && jsonObject.has("Expiration")) {
        } else {
            throw new CredentialException("Invalid json got from ECS Metadata service.");
        }

        if (!"Success".equals(jsonObject.get("Code").getAsString())) {
            throw new CredentialException(ECS_METADAT_FETCH_ERROR_MSG);
        }
        return new EcsRamRoleCredential(jsonObject.get("AccessKeyId").getAsString(), jsonObject.get(
                "AccessKeySecret").getAsString(), jsonObject.get("SecurityToken").getAsString(), jsonObject.get(
                "Expiration").getAsString());
    }

    public URL getCredentialUrl() {
        return credentialUrl;
    }

    public String getRoleName() {
        return roleName;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }
}