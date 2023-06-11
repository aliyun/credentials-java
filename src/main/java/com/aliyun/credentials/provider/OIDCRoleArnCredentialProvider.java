package com.aliyun.credentials.provider;

import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.*;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.models.Credential;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.utils.ParameterHelper;
import com.aliyun.credentials.utils.StringUtils;
import com.aliyun.tea.utils.Validate;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

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

    private String oidcToken;
    private String oidcTokenFilePath;

    /**
     * An identifier for the assumed role session.
     */
    private String roleSessionName = "defaultSessionName";

    private String accessKeyId;
    private String accessKeySecret;
    private String regionId = "cn-hangzhou";
    private String policy;

    /**
     * Unit of millisecond
     */
    private int connectTimeout = 1000;
    private int readTimeout = 1000;

    /**
     * Endpoint of RAM OpenAPI
     */
    private String STSEndpoint = "sts.aliyuncs.com";

    @Deprecated
    public OIDCRoleArnCredentialProvider(Configuration config) {
        this(config.getRoleArn(),
                config.getOIDCProviderArn(), config.getOIDCTokenFilePath());
        this.roleSessionName = config.getRoleSessionName();
        this.connectTimeout = config.getConnectTimeout();
        this.readTimeout = config.getReadTimeout();
        if (!StringUtils.isEmpty(config.getSTSEndpoint())) {
            this.STSEndpoint = config.getSTSEndpoint();
        }
    }

    @Deprecated
    public OIDCRoleArnCredentialProvider(Config config) {
        this(config.roleArn, config.oidcProviderArn, config.oidcTokenFilePath);
        this.roleSessionName = config.roleSessionName;
        this.connectTimeout = config.connectTimeout;
        this.readTimeout = config.timeout;
        this.policy = config.policy;
        this.durationSeconds = config.roleSessionExpiration;
        if (!StringUtils.isEmpty(config.STSEndpoint)) {
            this.STSEndpoint = config.STSEndpoint;
        }
    }

    @Deprecated
    public OIDCRoleArnCredentialProvider(String accessKeyId, String accessKeySecret, String roleArn,
                                         String oidcProviderArn, String oidcTokenFilePath) {
        this(roleArn, oidcProviderArn, oidcTokenFilePath);
    }

    @Deprecated
    public OIDCRoleArnCredentialProvider(String roleArn, String oidcProviderArn, String oidcTokenFilePath) {
        super(new BuilderImpl());
        if (!StringUtils.isEmpty(roleArn)) {
            this.roleArn = roleArn;
        } else if (!StringUtils.isEmpty(System.getenv("ALIBABA_CLOUD_ROLE_ARN"))) {
            this.roleArn = System.getenv("ALIBABA_CLOUD_ROLE_ARN");
        } else {
            throw new CredentialException("roleArn does not exist and env ALIBABA_CLOUD_ROLE_ARN is null.");
        }
        if (!StringUtils.isEmpty(oidcProviderArn)) {
            this.oidcProviderArn = oidcProviderArn;
        } else if (!StringUtils.isEmpty(System.getenv("ALIBABA_CLOUD_OIDC_PROVIDER_ARN"))) {
            this.oidcProviderArn = System.getenv("ALIBABA_CLOUD_OIDC_PROVIDER_ARN");
        } else {
            throw new CredentialException("OIDCProviderArn does not exist and env ALIBABA_CLOUD_OIDC_PROVIDER_ARN is null.");
        }
        if (!StringUtils.isEmpty(oidcTokenFilePath)) {
            this.oidcTokenFilePath = oidcTokenFilePath;
        } else if (!StringUtils.isEmpty(System.getenv("ALIBABA_CLOUD_OIDC_TOKEN_FILE"))) {
            this.oidcTokenFilePath = System.getenv("ALIBABA_CLOUD_OIDC_TOKEN_FILE");
        } else {
            throw new CredentialException("OIDCTokenFilePath does not exist and env ALIBABA_CLOUD_OIDC_TOKEN_FILE is null.");
        }
        if (!StringUtils.isEmpty(System.getenv("ALIBABA_CLOUD_ROLE_SESSION_NAME"))) {
            this.roleSessionName = System.getenv("ALIBABA_CLOUD_ROLE_SESSION_NAME");
        }
    }

    @Deprecated
    public OIDCRoleArnCredentialProvider(String accessKeyId, String accessKeySecret, String roleSessionName,
                                         String roleArn, String oidcProviderArn, String oidcTokenFilePath,
                                         String regionId, String policy) {
        this(roleSessionName, roleArn, oidcProviderArn, oidcTokenFilePath, regionId, policy);
    }

    @Deprecated
    public OIDCRoleArnCredentialProvider(String roleSessionName, String roleArn,
                                         String oidcProviderArn, String oidcTokenFilePath,
                                         String regionId, String policy) {
        this(roleArn, oidcProviderArn, oidcTokenFilePath);
        this.roleSessionName = roleSessionName;
        this.regionId = regionId;
        this.policy = policy;
    }

    private OIDCRoleArnCredentialProvider(BuilderImpl builder) {
        super(builder);
        this.roleSessionName = builder.roleSessionName;
        this.durationSeconds = builder.durationSeconds;
        this.roleArn = Validate.notNull(builder.roleArn, "RoleArn or environment variable ALIBABA_CLOUD_ROLE_ARN cannot be null.");
        this.oidcProviderArn = Validate.notNull(builder.oidcProviderArn, "OIDCProviderArn or environment variable ALIBABA_CLOUD_OIDC_PROVIDER_ARN cannot be null.");
        this.oidcTokenFilePath = Validate.notNull(builder.oidcTokenFilePath, "OIDCTokenFilePath or environment variable ALIBABA_CLOUD_OIDC_TOKEN_FILE cannot be null.");
        this.regionId = builder.regionId;
        this.policy = builder.policy;
        this.connectTimeout = builder.connectionTimeout;
        this.readTimeout = builder.readTimeout;
        this.STSEndpoint = builder.STSEndpoint;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public RefreshResult<Credential> refreshCredentials() {
        CompatibleUrlConnClient client = new CompatibleUrlConnClient();
        return createCredential(client);
    }

    public RefreshResult<Credential> createCredential(CompatibleUrlConnClient client) {
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
    public RefreshResult<Credential> getNewSessionCredentials(CompatibleUrlConnClient client) throws UnsupportedEncodingException {
        this.oidcToken = AuthUtils.getOIDCToken(oidcTokenFilePath);
        ParameterHelper parameterHelper = new ParameterHelper();
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setUrlParameter("Action", "AssumeRoleWithOIDC");
        httpRequest.setUrlParameter("Format", "JSON");
        httpRequest.setUrlParameter("Version", "2015-04-01");
        httpRequest.setUrlParameter("RegionId", this.regionId);
        Map<String, String> body = new HashMap<String, String>();
        body.put("DurationSeconds", String.valueOf(durationSeconds));
        body.put("RoleArn", this.roleArn);
        body.put("OIDCProviderArn", this.oidcProviderArn);
        body.put("OIDCToken", this.oidcToken);
        body.put("RoleSessionName", this.roleSessionName);
        body.put("Policy", this.policy);
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
        httpRequest.setSysUrl(parameterHelper.composeUrl(this.STSEndpoint, httpRequest.getUrlParameters(),
                "https"));
        HttpResponse httpResponse = client.syncInvoke(httpRequest);
        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(httpResponse.getHttpContentString(), Map.class);
        if (null == map) {
            throw new CredentialException(httpResponse.getResponseMessage());
        } else if (map.containsKey("Credentials")) {
            Map<String, String> result = (Map<String, String>) map.get("Credentials");
            long expiration = ParameterHelper.getUTCDate(result.get("Expiration")).getTime();
            Credential credential = Credential.builder()
                    .accessKeyId(result.get("AccessKeyId"))
                    .accessKeySecret(result.get("AccessKeySecret"))
                    .securityToken(result.get("SecurityToken"))
                    .type(AuthConstant.OIDC_ROLE_ARN)
                    .expiration(expiration)
                    .build();
            return RefreshResult.builder(credential)
                    .staleTime(getStaleTime(expiration))
                    .build();
        } else {
            throw new CredentialException(gson.toJson(map));
        }
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

    @Deprecated
    public String getAccessKeyId() {
        return accessKeyId;
    }

    @Deprecated
    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    @Deprecated
    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    @Deprecated
    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
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
        return STSEndpoint;
    }

    public void setSTSEndpoint(String STSEndpoint) {
        this.STSEndpoint = STSEndpoint;
    }

    public interface Builder extends SessionCredentialsProvider.Builder<OIDCRoleArnCredentialProvider, Builder> {
        Builder roleSessionName(String roleSessionName);

        Builder durationSeconds(int durationSeconds);

        Builder roleArn(String roleArn);

        Builder oidcProviderArn(String oidcProviderArn);

        Builder oidcTokenFilePath(String oidcTokenFilePath);

        Builder regionId(String regionId);

        Builder policy(String policy);

        Builder connectionTimeout(int connectionTimeout);

        Builder readTimeout(int readTimeout);

        Builder STSEndpoint(String STSEndpoint);

        @Override
        OIDCRoleArnCredentialProvider build();
    }

    private static final class BuilderImpl
            extends SessionCredentialsProvider.BuilderImpl<OIDCRoleArnCredentialProvider, Builder>
            implements Builder {
        private String roleSessionName = StringUtils.isEmpty(System.getenv("ALIBABA_CLOUD_ROLE_SESSION_NAME")) ?
                "defaultSessionName"
                : System.getenv("ALIBABA_CLOUD_ROLE_SESSION_NAME");
        private int durationSeconds = 3600;
        private String roleArn = System.getenv("ALIBABA_CLOUD_ROLE_ARN");
        private String oidcProviderArn = System.getenv("ALIBABA_CLOUD_OIDC_PROVIDER_ARN");
        private String oidcTokenFilePath = System.getenv("ALIBABA_CLOUD_OIDC_TOKEN_FILE");
        private String regionId = "cn-hangzhou";
        private String policy;
        private int connectionTimeout = 1000;
        private int readTimeout = 1000;
        private String STSEndpoint = "sts.aliyuncs.com";

        public Builder roleSessionName(String roleSessionName) {
            if (!StringUtils.isEmpty(roleSessionName)) {
                this.roleSessionName = roleSessionName;
            }
            return this;
        }

        public Builder durationSeconds(int durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public Builder roleArn(String roleArn) {
            if (!StringUtils.isEmpty(roleArn)) {
                this.roleArn = roleArn;
            }
            return this;
        }

        public Builder oidcProviderArn(String oidcProviderArn) {
            if (!StringUtils.isEmpty(oidcProviderArn)) {
                this.oidcProviderArn = oidcProviderArn;
            }
            return this;
        }

        public Builder oidcTokenFilePath(String oidcTokenFilePath) {
            if (!StringUtils.isEmpty(oidcTokenFilePath)) {
                this.oidcTokenFilePath = oidcTokenFilePath;
            }
            return this;
        }

        public Builder regionId(String regionId) {
            if (!StringUtils.isEmpty(regionId)) {
                this.regionId = regionId;
            }
            return this;
        }

        public Builder policy(String policy) {
            this.policy = policy;
            return this;
        }

        public Builder connectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder STSEndpoint(String STSEndpoint) {
            this.STSEndpoint = STSEndpoint;
            return this;
        }

        @Override
        public OIDCRoleArnCredentialProvider build() {
            return new OIDCRoleArnCredentialProvider(this);
        }
    }
}