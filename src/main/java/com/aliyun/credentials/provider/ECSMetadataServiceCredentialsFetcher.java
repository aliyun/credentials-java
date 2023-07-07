package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.http.MethodType;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.ParameterHelper;
import com.google.gson.Gson;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class ECSMetadataServiceCredentialsFetcher {
    private static final String URL_IN_ECS_METADATA = "/latest/meta-data/ram/security-credentials/";
    private static final String ECS_METADAT_FETCH_ERROR_MSG = "Failed to get RAM session credentials from ECS metadata service.";
    private URL credentialUrl;
    private String roleName;
    private String metadataServiceHost = "100.100.100.200";
    private int connectionTimeout = 1000;
    private int readTimeout = 1000;


    public ECSMetadataServiceCredentialsFetcher(String roleName, int connectionTimeout, int readTimeout) {
        if (connectionTimeout > 1000) {
            this.connectionTimeout = connectionTimeout;
        }
        if (readTimeout > 1000) {
            this.readTimeout = readTimeout;
        }
        this.roleName = roleName;
        setCredentialUrl();
    }

    public ECSMetadataServiceCredentialsFetcher(String roleName) {
        this.roleName = roleName;
        setCredentialUrl();
    }


    private void setCredentialUrl() {
        try {
            this.credentialUrl = new URL("http://" + metadataServiceHost + URL_IN_ECS_METADATA + roleName);
        } catch (MalformedURLException e) {
            throw new CredentialException(e.getMessage(), e);
        }
    }

    public String fetchRoleName(CompatibleUrlConnClient client) {
        return getMetadata(client);
    }

    public String getMetadata(CompatibleUrlConnClient client) {
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

        if (response.getResponseCode() == 404) {
            throw new CredentialException("The role name was not found in the instance");
        }

        if (response.getResponseCode() != 200) {
            throw new CredentialException(ECS_METADAT_FETCH_ERROR_MSG + " HttpCode=" + response.getResponseCode());
        }

        return new String(response.getHttpContent());
    }

    public RefreshResult<CredentialModel> fetch(CompatibleUrlConnClient client) {
        String jsonContent = getMetadata(client);
        Map<String, String> result = new Gson().fromJson(jsonContent, Map.class);

        if (!"Success".equals(result.get("Code"))) {
            throw new CredentialException(ECS_METADAT_FETCH_ERROR_MSG);
        }
        long expiration = ParameterHelper.getUTCDate(result.get("Expiration")).getTime();
        CredentialModel credential = CredentialModel.builder()
                .accessKeyId(result.get("AccessKeyId"))
                .accessKeySecret(result.get("AccessKeySecret"))
                .securityToken(result.get("SecurityToken"))
                .type(AuthConstant.ECS_RAM_ROLE)
                .expiration(expiration)
                .build();
        return RefreshResult.builder(credential)
                .staleTime(expiration - 3 * 60 * 1000)
                .build();

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