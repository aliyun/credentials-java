package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.http.MethodType;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.ParameterHelper;
import com.aliyun.credentials.utils.ProviderName;
import com.aliyun.credentials.utils.StringUtils;
import com.google.gson.Gson;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

public class ECSMetadataServiceCredentialsFetcher {
    private static final String URL_IN_ECS_METADATA = "/latest/meta-data/ram/security-credentials/";
    private static final String URL_IN_METADATA_TOKEN = "/latest/api/token";
    private static final String ECS_METADATA_FETCH_ERROR_MSG = "Failed to get RAM session credentials from ECS metadata service.";
    private URL credentialUrl;
    private final String roleName;
    private final String metadataServiceHost = "100.100.100.200";
    private int connectionTimeout = 1000;
    private int readTimeout = 1000;
    private final boolean disableIMDSv1;
    private final int metadataTokenDuration = 21600;

    public ECSMetadataServiceCredentialsFetcher(String roleName, Integer connectionTimeout, Integer readTimeout) {
        this.connectionTimeout = connectionTimeout == null ? 1000 : connectionTimeout;
        this.readTimeout = readTimeout == null ? 1000 : readTimeout;
        this.disableIMDSv1 = false;
        this.roleName = roleName;
        setCredentialUrl();
    }

    @Deprecated
    public ECSMetadataServiceCredentialsFetcher(String roleName, Boolean disableIMDSv1, Integer metadataTokenDuration, Integer connectionTimeout, Integer readTimeout) {
        this.connectionTimeout = connectionTimeout == null ? 1000 : connectionTimeout;
        this.readTimeout = readTimeout == null ? 1000 : readTimeout;
        this.disableIMDSv1 = disableIMDSv1;
        this.roleName = roleName;
        setCredentialUrl();
    }

    public ECSMetadataServiceCredentialsFetcher(String roleName, Boolean disableIMDSv1, Integer connectionTimeout, Integer readTimeout) {
        this.connectionTimeout = connectionTimeout == null ? 1000 : connectionTimeout;
        this.readTimeout = readTimeout == null ? 1000 : readTimeout;
        this.disableIMDSv1 = disableIMDSv1 == null ? false : disableIMDSv1;
        this.roleName = roleName;
        setCredentialUrl();
    }

    public ECSMetadataServiceCredentialsFetcher(String roleName) {
        this.roleName = roleName;
        this.disableIMDSv1 = false;
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
        return getMetadata(client, credentialUrl.toString());
    }

    private String getMetadata(CompatibleUrlConnClient client, String url) {
        HttpRequest request = new HttpRequest(url);
        request.setSysMethod(MethodType.GET);
        request.setSysConnectTimeout(connectionTimeout);
        request.setSysReadTimeout(readTimeout);
        HttpResponse response;
        String metadataToken = this.getMetadataToken(client);
        if (metadataToken != null) {
            request.putHeaderParameter("X-aliyun-ecs-metadata-token", metadataToken);
        }

        try {
            response = client.syncInvoke(request);
        } catch (Exception e) {
            throw new CredentialException("Failed to connect ECS Metadata Service: " + e);
        }

        if (response.getResponseCode() == 404) {
            throw new CredentialException("The role name was not found in the instance.");
        }

        if (response.getResponseCode() != 200) {
            throw new CredentialException(ECS_METADATA_FETCH_ERROR_MSG + " HttpCode=" + response.getResponseCode());
        }

        return new String(response.getHttpContent());
    }

    public RefreshResult<CredentialModel> fetch(CompatibleUrlConnClient client) {
        String roleName = this.roleName;
        if (StringUtils.isEmpty(this.roleName)) {
            roleName = getMetadata(client, "http://" + metadataServiceHost + URL_IN_ECS_METADATA);
        }
        String jsonContent = getMetadata(client, "http://" + metadataServiceHost + URL_IN_ECS_METADATA + roleName);
        Map<String, String> result = new Gson().fromJson(jsonContent, Map.class);

        if (!"Success".equals(result.get("Code"))) {
            throw new CredentialException(ECS_METADATA_FETCH_ERROR_MSG);
        }
        if (!result.containsKey("AccessKeyId") || !result.containsKey("AccessKeySecret") || !result.containsKey("SecurityToken")) {
            throw new CredentialException(String.format("Error retrieving credentials from IMDS result: %s.", jsonContent));
        }
        long expiration = ParameterHelper.getUTCDate(result.get("Expiration")).getTime();
        CredentialModel credential = CredentialModel.builder()
                .accessKeyId(result.get("AccessKeyId"))
                .accessKeySecret(result.get("AccessKeySecret"))
                .securityToken(result.get("SecurityToken"))
                .type(AuthConstant.ECS_RAM_ROLE)
                .providerName(ProviderName.ECS_RAM_ROLE)
                .expiration(expiration)
                .build();
        return RefreshResult.builder(credential)
                .staleTime(getStaleTime(expiration))
                .prefetchTime(getPrefetchTime(expiration))
                .build();

    }

    private long getStaleTime(long expiration) {
        return expiration <= 0 ?
                new Date().getTime() + 60 * 60 * 1000
                : expiration - 15 * 60 * 1000;
    }

    private long getPrefetchTime(long expiration) {
        return expiration <= 0 ?
                new Date().getTime() + 5 * 60 * 1000
                : new Date().getTime() + 60 * 60 * 1000;
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

    public boolean getDisableIMDSv1() {
        return disableIMDSv1;
    }

    public int getMetadataTokenDuration() {
        return metadataTokenDuration;
    }

    private String getMetadataToken(CompatibleUrlConnClient client) {
        try {
            HttpRequest request = new HttpRequest("http://" + metadataServiceHost + URL_IN_METADATA_TOKEN);
            request.setSysMethod(MethodType.PUT);
            request.setSysConnectTimeout(connectionTimeout);
            request.setSysReadTimeout(readTimeout);
            request.putHeaderParameter("X-aliyun-ecs-metadata-token-ttl-seconds", String.valueOf(this.metadataTokenDuration));
            HttpResponse response;
            try {
                response = client.syncInvoke(request);
            } catch (Exception e) {
                throw new CredentialException("Failed to connect ECS Metadata Service: " + e);
            }
            if (response.getResponseCode() != 200) {
                throw new CredentialException("Failed to get token from ECS Metadata Service. HttpCode=" + response.getResponseCode() + ", ResponseMessage=" + response.getHttpContentString());
            }
            return new String(response.getHttpContent());
        } catch (Exception ex) {
            return throwErrorOrReturn(ex);
        }
    }

    private String throwErrorOrReturn(Exception e) {
        if (this.disableIMDSv1) {
            throw new CredentialException("Failed to get token from ECS Metadata Service, and fallback to IMDS v1 is disabled via the disableIMDSv1 configuration is turned on. Original error: " + e.getMessage());
        }
        return null;
    }
}