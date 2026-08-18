package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.*;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.ParameterHelper;
import com.aliyun.credentials.utils.ProviderName;
import com.aliyun.credentials.utils.StringUtils;
import com.google.gson.Gson;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class CloudSSOCredentialsProvider extends SessionCredentialsProvider {

    private final String signInUrl;
    private final String accountId;
    private final String accessConfig;
    private final String accessToken;
    private final long accessTokenExpire;
    private final int connectTimeout;
    private final int readTimeout;

    private CloudSSOCredentialsProvider(BuilderImpl builder) {
        super(builder);

        if (StringUtils.isEmpty(builder.accessToken) || builder.accessTokenExpire == 0
                || builder.accessTokenExpire - System.currentTimeMillis() / 1000 <= 0) {
            throw new IllegalArgumentException("CloudSSO access token is empty or expired, please re-login with cli.");
        }
        if (StringUtils.isEmpty(builder.signInUrl) || StringUtils.isEmpty(builder.accountId)
                || StringUtils.isEmpty(builder.accessConfig)) {
            throw new IllegalArgumentException("CloudSSO sign in url, account id, and access config cannot be empty.");
        }

        this.signInUrl = builder.signInUrl;
        this.accountId = builder.accountId;
        this.accessConfig = builder.accessConfig;
        this.accessToken = builder.accessToken;
        this.accessTokenExpire = builder.accessTokenExpire;
        this.connectTimeout = builder.connectTimeout == null ? 5000 : builder.connectTimeout;
        this.readTimeout = builder.readTimeout == null ? 10000 : builder.readTimeout;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public RefreshResult<CredentialModel> refreshCredentials() {
        try (CompatibleUrlConnClient client = new CompatibleUrlConnClient()) {
            return getNewSessionCredentials(client);
        }
    }

    RefreshResult<CredentialModel> getNewSessionCredentials(CompatibleUrlConnClient client) {
        URL parsedUrl;
        try {
            parsedUrl = new URL(this.signInUrl);
        } catch (MalformedURLException e) {
            throw new CredentialException("Invalid CloudSSO sign in url: " + e.getMessage(), e);
        }

        String requestUrl = parsedUrl.getProtocol() + "://" + parsedUrl.getHost() + "/cloud-credentials";
        String body = String.format("{\"AccountId\":\"%s\",\"AccessConfigurationId\":\"%s\"}",
                this.accountId, this.accessConfig);

        HttpRequest httpRequest = new HttpRequest(requestUrl);
        httpRequest.setSysMethod(MethodType.POST);
        httpRequest.setSysConnectTimeout(this.connectTimeout);
        httpRequest.setSysReadTimeout(this.readTimeout);
        httpRequest.setHttpContent(body.getBytes(), "UTF-8", FormatType.JSON);
        httpRequest.putHeaderParameter("Accept", "application/json");
        httpRequest.putHeaderParameter("Content-Type", "application/json");
        httpRequest.putHeaderParameter("Authorization", "Bearer " + this.accessToken);

        HttpResponse httpResponse;
        try {
            httpResponse = client.syncInvoke(httpRequest);
        } catch (Exception e) {
            throw new CredentialException("Failed to connect CloudSSO service: " + e.getMessage(), e);
        }

        if (httpResponse.getResponseCode() != 200) {
            throw new CredentialException(String.format(
                    "Get session token from CloudSSO failed, %s.",
                    httpResponse.toHttpFailureString()));
        }

        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(httpResponse.getHttpContentString(), Map.class);
        if (null == map || !map.containsKey("CloudCredential")) {
            throw new CredentialException(String.format(
                    "Get session token from CloudSSO failed, result: %s.", httpResponse.getHttpContentString()));
        }

        Map<String, String> result = (Map<String, String>) map.get("CloudCredential");
        if (result == null || !result.containsKey("AccessKeyId") || !result.containsKey("AccessKeySecret")
                || !result.containsKey("SecurityToken")) {
            throw new CredentialException(String.format(
                    "Get session token from CloudSSO failed, fail to get credentials: %s.",
                    httpResponse.getHttpContentString()));
        }

        long expiration = ParameterHelper.getUTCDate(result.get("Expiration")).getTime();
        CredentialModel credential = CredentialModel.builder()
                .accessKeyId(result.get("AccessKeyId"))
                .accessKeySecret(result.get("AccessKeySecret"))
                .securityToken(result.get("SecurityToken"))
                .type(AuthConstant.STS)
                .providerName(this.getProviderName())
                .expiration(expiration)
                .build();
        return RefreshResult.builder(credential)
                .staleTime(getStaleTime(expiration))
                .build();
    }

    @Override
    public String getProviderName() {
        return ProviderName.CLOUD_SSO;
    }

    @Override
    public void close() {
        super.close();
    }

    public interface Builder extends SessionCredentialsProvider.Builder<CloudSSOCredentialsProvider, Builder> {
        Builder signInUrl(String signInUrl);

        Builder accountId(String accountId);

        Builder accessConfig(String accessConfig);

        Builder accessToken(String accessToken);

        Builder accessTokenExpire(long accessTokenExpire);

        Builder connectTimeout(Integer connectTimeout);

        Builder readTimeout(Integer readTimeout);

        @Override
        CloudSSOCredentialsProvider build();
    }

    private static final class BuilderImpl
            extends SessionCredentialsProvider.BuilderImpl<CloudSSOCredentialsProvider, Builder>
            implements Builder {
        private String signInUrl;
        private String accountId;
        private String accessConfig;
        private String accessToken;
        private long accessTokenExpire;
        private Integer connectTimeout;
        private Integer readTimeout;

        @Override
        public Builder signInUrl(String signInUrl) {
            this.signInUrl = signInUrl;
            return this;
        }

        @Override
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        @Override
        public Builder accessConfig(String accessConfig) {
            this.accessConfig = accessConfig;
            return this;
        }

        @Override
        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        @Override
        public Builder accessTokenExpire(long accessTokenExpire) {
            this.accessTokenExpire = accessTokenExpire;
            return this;
        }

        @Override
        public Builder connectTimeout(Integer connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        @Override
        public Builder readTimeout(Integer readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        @Override
        public CloudSSOCredentialsProvider build() {
            return new CloudSSOCredentialsProvider(this);
        }
    }
}
