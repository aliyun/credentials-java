package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.*;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.*;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static com.aliyun.credentials.configure.Config.ENDPOINT_SUFFIX;
import static com.aliyun.credentials.configure.Config.STS_DEFAULT_ENDPOINT;

public class OIDCRoleArnCredentialProvider extends SessionCredentialsProvider {

    /**
     * Default duration for started sessions. Unit of Second
     */
    public int durationSeconds = 3600;
    /**
     * The arn of the role to be assumed.
     */
    private String roleArn;
    private String oidcProviderArn;

    private volatile String oidcToken;
    private String oidcTokenFilePath;

    /**
     * An identifier for the assumed role session.
     */
    private String roleSessionName;

    private String policy;

    /**
     * Unit of millisecond
     */
    private int connectTimeout;
    private int readTimeout;

    /**
     * Endpoint of RAM OpenAPI
     */
    private String stsEndpoint;

    private OIDCRoleArnCredentialProvider(BuilderImpl builder) {
        super(builder);
        this.roleSessionName = builder.roleSessionName == null ? !StringUtils.isEmpty(AuthUtils.getEnvironmentRoleSessionName()) ?
                AuthUtils.getEnvironmentRoleSessionName() : "credentials-java-" + System.currentTimeMillis() : builder.roleSessionName;
        this.durationSeconds = builder.durationSeconds == null ? 3600 : builder.durationSeconds;
        if (this.durationSeconds < 900) {
            throw new IllegalArgumentException("Session duration should be in the range of 900s - max session duration.");
        }

        this.roleArn = builder.roleArn == null ? AuthUtils.getEnvironmentRoleArn() : builder.roleArn;
        if (StringUtils.isEmpty(this.roleArn)) {
            throw new IllegalArgumentException("RoleArn or environment variable ALIBABA_CLOUD_ROLE_ARN cannot be empty.");
        }

        this.oidcProviderArn = builder.oidcProviderArn == null ? AuthUtils.getEnvironmentOIDCProviderArn() : builder.oidcProviderArn;
        if (StringUtils.isEmpty(this.oidcProviderArn)) {
            throw new IllegalArgumentException("OIDCProviderArn or environment variable ALIBABA_CLOUD_OIDC_PROVIDER_ARN cannot be empty.");
        }

        this.oidcTokenFilePath = builder.oidcTokenFilePath == null ? AuthUtils.getEnvironmentOIDCTokenFilePath() : builder.oidcTokenFilePath;
        if (StringUtils.isEmpty(this.oidcTokenFilePath)) {
            throw new IllegalArgumentException("OIDCTokenFilePath or environment variable ALIBABA_CLOUD_OIDC_TOKEN_FILE cannot be empty.");
        }

        this.policy = builder.policy;
        this.connectTimeout = builder.connectionTimeout == null ? 5000 : builder.connectionTimeout;
        this.readTimeout = builder.readTimeout == null ? 10000 : builder.readTimeout;

        if (!StringUtils.isEmpty(builder.stsEndpoint)) {
            this.stsEndpoint = builder.stsEndpoint;
        } else {
            String prefix = builder.enableVpc != null ? (builder.enableVpc ? "sts-vpc" : "sts") : AuthUtils.isEnableVpcEndpoint() ? "sts-vpc" : "sts";
            if (!StringUtils.isEmpty(builder.stsRegionId)) {
                this.stsEndpoint = String.format("%s.%s.%s", prefix, builder.stsRegionId, ENDPOINT_SUFFIX);
            } else if (!StringUtils.isEmpty(AuthUtils.getEnvironmentSTSRegion())) {
                this.stsEndpoint = String.format("%s.%s.%s", prefix, AuthUtils.getEnvironmentSTSRegion(), ENDPOINT_SUFFIX);
            } else {
                this.stsEndpoint = STS_DEFAULT_ENDPOINT;
            }
        }
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public RefreshResult<CredentialModel> refreshCredentials() {
        try (CompatibleUrlConnClient client = new CompatibleUrlConnClient()) {
            return createCredential(client);
        }
    }

    public RefreshResult<CredentialModel> createCredential(CompatibleUrlConnClient client) {
        try {
            return getNewSessionCredentials(client);
        } catch (UnsupportedEncodingException e) {
            throw new CredentialException(e.getMessage(), e);
        } finally {
            client.close();
        }
    }

    public RefreshResult<CredentialModel> getNewSessionCredentials(CompatibleUrlConnClient client) throws UnsupportedEncodingException {
        String token = AuthUtils.getOIDCToken(oidcTokenFilePath);
        this.oidcToken = token;
        ParameterHelper parameterHelper = new ParameterHelper();
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setUrlParameter("Action", "AssumeRoleWithOIDC");
        httpRequest.setUrlParameter("Format", "JSON");
        httpRequest.setUrlParameter("Version", "2015-04-01");
        Map<String, String> body = new HashMap<String, String>();
        body.put("DurationSeconds", String.valueOf(durationSeconds));
        body.put("RoleArn", this.roleArn);
        body.put("OIDCProviderArn", this.oidcProviderArn);
        body.put("OIDCToken", token);
        body.put("RoleSessionName", this.roleSessionName);
        if (policy != null) {
            body.put("Policy", this.policy);
        }
        StringBuilder content = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : body.entrySet()) {
            if (StringUtils.isEmpty(entry.getValue())) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                content.append("&");
            }
            content.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            content.append("=");
            content.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        httpRequest.setHttpContent(content.toString().getBytes("UTF-8"), "UTF-8", FormatType.FORM);
        httpRequest.setSysMethod(MethodType.POST);
        httpRequest.setSysConnectTimeout(this.connectTimeout);
        httpRequest.setSysReadTimeout(this.readTimeout);
        httpRequest.setSysUrl(parameterHelper.composeUrl(this.stsEndpoint, httpRequest.getUrlParameters(),
                "https"));
        HttpResponse httpResponse;
        try {
            httpResponse = client.syncInvoke(httpRequest);
        } catch (Exception e) {
            throw new CredentialException("Failed to connect OIDC Service: " + e);
        }
        if (httpResponse.getResponseCode() != 200) {
            throw new CredentialException(String.format("Error refreshing credentials from OIDC, HttpCode: %s, result: %s.", httpResponse.getResponseCode(), httpResponse.getHttpContentString()));
        }

        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(httpResponse.getHttpContentString(), Map.class);
        if (null == map || !map.containsKey("Credentials")) {
            throw new CredentialException(String.format("Error retrieving credentials from OIDC result: %s.", httpResponse.getHttpContentString()));
        }
        Map<String, String> result = (Map<String, String>) map.get("Credentials");
        if (!result.containsKey("AccessKeyId") || !result.containsKey("AccessKeySecret") || !result.containsKey("SecurityToken")) {
            throw new CredentialException(String.format("Error retrieving credentials from OIDC result: %s.", httpResponse.getHttpContentString()));
        }
        long expiration = ParameterHelper.getUTCDate(result.get("Expiration")).getTime();
        CredentialModel credential = CredentialModel.builder()
                .accessKeyId(result.get("AccessKeyId"))
                .accessKeySecret(result.get("AccessKeySecret"))
                .securityToken(result.get("SecurityToken"))
                .type(AuthConstant.OIDC_ROLE_ARN)
                .providerName(this.getProviderName())
                .expiration(expiration)
                .build();
        return RefreshResult.builder(credential)
                .staleTime(getStaleTime(expiration))
                .build();

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

    public String getOIDCProviderArn() {
        return oidcProviderArn;
    }

    public String getOIDCToken() {
        return oidcToken;
    }

    public String getOIDCTokenFilePath() {
        return oidcTokenFilePath;
    }

    public String getRoleSessionName() {
        return roleSessionName;
    }

    public void setRoleSessionName(String roleSessionName) {
        this.roleSessionName = roleSessionName;
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

    public String getSTSEndpoint() {
        return stsEndpoint;
    }

    public void setSTSEndpoint(String stsEndpoint) {
        this.stsEndpoint = stsEndpoint;
    }

    @Override
    public String getProviderName() {
        return ProviderName.OIDC_ROLE_ARN;
    }

    @Override
    public void close() {
        super.close();
    }

    public interface Builder extends SessionCredentialsProvider.Builder<OIDCRoleArnCredentialProvider, Builder> {
        Builder roleSessionName(String roleSessionName);

        Builder durationSeconds(Integer durationSeconds);

        Builder roleArn(String roleArn);

        Builder oidcProviderArn(String oidcProviderArn);

        Builder oidcTokenFilePath(String oidcTokenFilePath);

        Builder policy(String policy);

        Builder connectionTimeout(Integer connectionTimeout);

        Builder readTimeout(Integer readTimeout);

        Builder stsEndpoint(String STSEndpoint);

        Builder stsRegionId(String stsRegionId);

        Builder enableVpc(Boolean enableVpc);

        @Override
        OIDCRoleArnCredentialProvider build();
    }

    private static final class BuilderImpl
            extends SessionCredentialsProvider.BuilderImpl<OIDCRoleArnCredentialProvider, Builder>
            implements Builder {
        private String roleSessionName;
        private Integer durationSeconds;
        private String roleArn;
        private String oidcProviderArn;
        private String oidcTokenFilePath;
        private String policy;
        private Integer connectionTimeout;
        private Integer readTimeout;
        private String stsEndpoint;
        private String stsRegionId;
        private Boolean enableVpc;

        public Builder roleSessionName(String roleSessionName) {
            this.roleSessionName = roleSessionName;
            return this;
        }

        public Builder durationSeconds(Integer durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public Builder roleArn(String roleArn) {
            this.roleArn = roleArn;
            return this;
        }

        public Builder oidcProviderArn(String oidcProviderArn) {
            this.oidcProviderArn = oidcProviderArn;
            return this;
        }

        public Builder oidcTokenFilePath(String oidcTokenFilePath) {
            this.oidcTokenFilePath = oidcTokenFilePath;
            return this;
        }

        public Builder policy(String policy) {
            this.policy = policy;
            return this;
        }

        public Builder connectionTimeout(Integer connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder readTimeout(Integer readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder stsEndpoint(String stsEndpoint) {
            this.stsEndpoint = stsEndpoint;
            return this;
        }

        public Builder stsRegionId(String stsRegionId) {
            this.stsRegionId = stsRegionId;
            return this;
        }

        public Builder enableVpc(Boolean enableVpc) {
            this.enableVpc = enableVpc;
            return this;
        }

        @Override
        public OIDCRoleArnCredentialProvider build() {
            return new OIDCRoleArnCredentialProvider(this);
        }
    }
}