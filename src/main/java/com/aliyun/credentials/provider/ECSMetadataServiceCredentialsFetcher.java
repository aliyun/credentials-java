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
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ECSMetadataServiceCredentialsFetcher {
    private static final String URL_IN_ECS_METADATA = "/latest/meta-data/ram/security-credentials/";
    private static final String URL_IN_METADATA_TOKEN = "/latest/api/token";
    private static final String ECS_METADAT_FETCH_ERROR_MSG = "Failed to get RAM session credentials from ECS metadata service.";
    private URL credentialUrl;
    private String roleName;
    private String metadataServiceHost = "100.100.100.200";
    private int connectionTimeout = 1000;
    private int readTimeout = 1000;
    private String metadataToken;
    private final boolean enableIMDSv2;
    private int metadataTokenDuration;
    private volatile long staleTime;

    /**
     * Maximum time to wait for a blocking refresh lock before calling refresh again. Unit of milliseconds.
     */
    private static final long REFRESH_BLOCKING_MAX_WAIT = 5 * 1000;
    private final Lock refreshLock = new ReentrantLock();


    public ECSMetadataServiceCredentialsFetcher(String roleName, int connectionTimeout, int readTimeout) {
        if (connectionTimeout > 1000) {
            this.connectionTimeout = connectionTimeout;
        }
        if (readTimeout > 1000) {
            this.readTimeout = readTimeout;
        }
        this.enableIMDSv2 = false;
        this.roleName = roleName;
        setCredentialUrl();
    }

    public ECSMetadataServiceCredentialsFetcher(String roleName, boolean enableIMDSv2, int metadataTokenDuration, int connectionTimeout, int readTimeout) {
        if (connectionTimeout > 1000) {
            this.connectionTimeout = connectionTimeout;
        }
        if (readTimeout > 1000) {
            this.readTimeout = readTimeout;
        }
        this.enableIMDSv2 = enableIMDSv2;
        this.metadataTokenDuration = metadataTokenDuration;
        this.roleName = roleName;
        setCredentialUrl();
    }

    public ECSMetadataServiceCredentialsFetcher(String roleName) {
        this.roleName = roleName;
        this.enableIMDSv2 = false;
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
        if (this.enableIMDSv2) {
            refreshMetadataToken(client);
            request.putHeaderParameter("X-aliyun-ecs-metadata-token", this.metadataToken);
        }

        try {
            response = client.syncInvoke(request);
        } catch (Exception e) {
            throw new CredentialException("Failed to connect ECS Metadata Service: " + e);
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

    public boolean getEnableIMDSv2() {
        return enableIMDSv2;
    }

    public int getMetadataTokenDuration() {
        return metadataTokenDuration;
    }

    private void refreshMetadataToken(CompatibleUrlConnClient client) {
        try {
            if (refreshLock.tryLock(REFRESH_BLOCKING_MAX_WAIT, TimeUnit.MILLISECONDS)) {
                try {
                    if (needToRefresh()) {
                        HttpRequest request = new HttpRequest("http://" + metadataServiceHost + URL_IN_METADATA_TOKEN);
                        request.setSysMethod(MethodType.PUT);
                        request.setSysConnectTimeout(connectionTimeout);
                        request.setSysReadTimeout(readTimeout);
                        HttpResponse response;
                        request.putHeaderParameter("X-aliyun-ecs-metadata-token-ttl-seconds", String.valueOf(this.metadataTokenDuration));
                        long tmpTime = this.staleTime;
                        this.staleTime = new Date().getTime() + this.metadataTokenDuration * 1000L - REFRESH_BLOCKING_MAX_WAIT * 2;

                        try {
                            response = client.syncInvoke(request);
                        } catch (Exception e) {
                            this.staleTime = tmpTime;
                            throw new CredentialException("Failed to connect ECS Metadata Service: " + e);
                        }

                        if (response.getResponseCode() != 200) {
                            this.staleTime = tmpTime;
                            throw new CredentialException("Failed to get token from ECS Metadata Service. HttpCode=" + response.getResponseCode());
                        }

                        this.metadataToken = new String(response.getHttpContent());
                    }
                } finally {
                    refreshLock.unlock();
                }
            }
        } catch (InterruptedException ex) {
            throw new IllegalStateException("Interrupted waiting to refresh the metadata token.", ex);
        } catch (Exception ex) {
            throw ex;
        }
    }

    private boolean needToRefresh() {
        return new Date().getTime() >= this.staleTime;
    }
}