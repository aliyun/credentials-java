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
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.SimpleTimeZone;

public class OAuthCredentialsProvider extends SessionCredentialsProvider {

    @FunctionalInterface
    public interface OAuthTokenUpdateCallback {
        void onTokenUpdate(String refreshToken, String accessToken, String accessKeyId,
                           String accessKeySecret, String securityToken,
                           long accessTokenExpire, long stsExpire) throws Exception;
    }

    private final String clientId;
    private final String signInUrl;
    private volatile String refreshToken;
    private volatile String accessToken;
    private volatile long accessTokenExpire;
    private final int connectTimeout;
    private final int readTimeout;
    private final OAuthTokenUpdateCallback tokenUpdateCallback;

    private OAuthCredentialsProvider(BuilderImpl builder) {
        super(builder);

        if (StringUtils.isEmpty(builder.clientId)) {
            throw new IllegalArgumentException("The clientId is empty.");
        }
        if (StringUtils.isEmpty(builder.signInUrl)) {
            throw new IllegalArgumentException("The url for sign-in is empty.");
        }

        this.clientId = builder.clientId;
        this.signInUrl = builder.signInUrl;
        this.refreshToken = builder.refreshToken;
        this.accessToken = builder.accessToken;
        this.accessTokenExpire = builder.accessTokenExpire;
        this.connectTimeout = builder.connectTimeout == null ? 5000 : builder.connectTimeout;
        this.readTimeout = builder.readTimeout == null ? 10000 : builder.readTimeout;
        this.tokenUpdateCallback = builder.tokenUpdateCallback;
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
        long nowSeconds = System.currentTimeMillis() / 1000;
        if (!StringUtils.isEmpty(this.refreshToken)
                && (StringUtils.isEmpty(this.accessToken) || this.accessTokenExpire == 0
                || this.accessTokenExpire - nowSeconds <= 1200)) {
            tryRefreshOAuthToken(client);
        }

        URL parsedUrl;
        try {
            parsedUrl = new URL(this.signInUrl);
        } catch (MalformedURLException e) {
            throw new CredentialException("Invalid OAuth sign in url: " + e.getMessage(), e);
        }

        String requestUrl = parsedUrl.getProtocol() + "://" + parsedUrl.getHost() + "/v1/exchange";

        HttpRequest httpRequest = new HttpRequest(requestUrl);
        httpRequest.setSysMethod(MethodType.POST);
        httpRequest.setSysConnectTimeout(this.connectTimeout);
        httpRequest.setSysReadTimeout(this.readTimeout);
        httpRequest.putHeaderParameter("Content-Type", "application/json");
        httpRequest.putHeaderParameter("Authorization", "Bearer " + this.accessToken);

        HttpResponse httpResponse;
        try {
            httpResponse = client.syncInvoke(httpRequest);
        } catch (Exception e) {
            throw new CredentialException("Failed to connect OAuth service: " + e.getMessage(), e);
        }

        if (httpResponse.getResponseCode() != 200) {
            throw new CredentialException(String.format(
                    "Get session token from OAuth failed, HttpCode: %s, result: %s.",
                    httpResponse.getResponseCode(), httpResponse.getHttpContentString()));
        }

        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(httpResponse.getHttpContentString(), Map.class);
        if (null == map) {
            throw new CredentialException(String.format(
                    "Get session token from OAuth failed, result: %s.", httpResponse.getHttpContentString()));
        }

        String accessKeyId = (String) map.get("accessKeyId");
        String accessKeySecret = (String) map.get("accessKeySecret");
        String securityToken = (String) map.get("securityToken");
        String expiration = (String) map.get("expiration");

        if (StringUtils.isEmpty(accessKeyId) || StringUtils.isEmpty(accessKeySecret)
                || StringUtils.isEmpty(securityToken)) {
            throw new CredentialException(String.format(
                    "Refresh session token from OAuth failed, fail to get credentials: %s.",
                    httpResponse.getHttpContentString()));
        }

        long expirationMs = ParameterHelper.getUTCDate(expiration).getTime();

        if (this.tokenUpdateCallback != null) {
            try {
                this.tokenUpdateCallback.onTokenUpdate(this.refreshToken, this.accessToken,
                        accessKeyId, accessKeySecret, securityToken,
                        this.accessTokenExpire, expirationMs / 1000);
            } catch (Exception e) {
                // Warning only, do not break credential retrieval
            }
        }

        CredentialModel credential = CredentialModel.builder()
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .securityToken(securityToken)
                .type(AuthConstant.STS)
                .providerName(this.getProviderName())
                .expiration(expirationMs)
                .build();
        return RefreshResult.builder(credential)
                .staleTime(getStaleTime(expirationMs))
                .build();
    }

    private void tryRefreshOAuthToken(CompatibleUrlConnClient client) {
        URL parsedUrl;
        try {
            parsedUrl = new URL(this.signInUrl);
        } catch (MalformedURLException e) {
            throw new CredentialException("Invalid OAuth sign in url: " + e.getMessage(), e);
        }

        String requestUrl = parsedUrl.getProtocol() + "://" + parsedUrl.getHost() + "/v1/token";

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(new SimpleTimeZone(0, "UTC"));
        String timestamp = df.format(new Date());

        String body;
        try {
            body = "grant_type=refresh_token"
                    + "&refresh_token=" + URLEncoder.encode(this.refreshToken, "UTF-8")
                    + "&client_id=" + URLEncoder.encode(this.clientId, "UTF-8")
                    + "&Timestamp=" + URLEncoder.encode(timestamp, "UTF-8");
        } catch (Exception e) {
            throw new CredentialException("Failed to encode token refresh request: " + e.getMessage(), e);
        }

        HttpRequest httpRequest = new HttpRequest(requestUrl);
        httpRequest.setSysMethod(MethodType.POST);
        httpRequest.setSysConnectTimeout(this.connectTimeout);
        httpRequest.setSysReadTimeout(this.readTimeout);
        httpRequest.setHttpContent(body.getBytes(), "UTF-8", FormatType.FORM);
        httpRequest.putHeaderParameter("Content-Type", "application/x-www-form-urlencoded");

        HttpResponse httpResponse;
        try {
            httpResponse = client.syncInvoke(httpRequest);
        } catch (Exception e) {
            throw new CredentialException("Failed to refresh OAuth token: " + e.getMessage(), e);
        }

        if (httpResponse.getResponseCode() != 200) {
            throw new CredentialException(String.format(
                    "Failed to refresh OAuth token, status code: %d, result: %s.",
                    httpResponse.getResponseCode(), httpResponse.getHttpContentString()));
        }

        Gson gson = new Gson();
        Map<String, Object> tokenResp = gson.fromJson(httpResponse.getHttpContentString(), Map.class);
        if (tokenResp == null) {
            throw new CredentialException("Failed to refresh OAuth token: empty response.");
        }

        String newAccessToken = (String) tokenResp.get("access_token");
        String newRefreshToken = (String) tokenResp.get("refresh_token");
        Double expiresIn = (Double) tokenResp.get("expires_in");

        if (StringUtils.isEmpty(newAccessToken) || StringUtils.isEmpty(newRefreshToken)) {
            throw new CredentialException(String.format(
                    "Failed to refresh OAuth token: %s.", httpResponse.getHttpContentString()));
        }

        this.accessToken = newAccessToken;
        this.refreshToken = newRefreshToken;
        this.accessTokenExpire = System.currentTimeMillis() / 1000 + (expiresIn != null ? expiresIn.longValue() : 3600);
    }

    @Override
    public String getProviderName() {
        return ProviderName.OAUTH;
    }

    @Override
    public void close() {
        super.close();
    }

    public interface Builder extends SessionCredentialsProvider.Builder<OAuthCredentialsProvider, Builder> {
        Builder clientId(String clientId);

        Builder signInUrl(String signInUrl);

        Builder refreshToken(String refreshToken);

        Builder accessToken(String accessToken);

        Builder accessTokenExpire(long accessTokenExpire);

        Builder connectTimeout(Integer connectTimeout);

        Builder readTimeout(Integer readTimeout);

        Builder tokenUpdateCallback(OAuthTokenUpdateCallback callback);

        @Override
        OAuthCredentialsProvider build();
    }

    private static final class BuilderImpl
            extends SessionCredentialsProvider.BuilderImpl<OAuthCredentialsProvider, Builder>
            implements Builder {
        private String clientId;
        private String signInUrl;
        private String refreshToken;
        private String accessToken;
        private long accessTokenExpire;
        private Integer connectTimeout;
        private Integer readTimeout;
        private OAuthTokenUpdateCallback tokenUpdateCallback;

        @Override
        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        @Override
        public Builder signInUrl(String signInUrl) {
            this.signInUrl = signInUrl;
            return this;
        }

        @Override
        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
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
        public Builder tokenUpdateCallback(OAuthTokenUpdateCallback callback) {
            this.tokenUpdateCallback = callback;
            return this;
        }

        @Override
        public OAuthCredentialsProvider build() {
            return new OAuthCredentialsProvider(this);
        }
    }
}
