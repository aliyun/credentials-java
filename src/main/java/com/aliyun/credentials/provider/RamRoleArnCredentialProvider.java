package com.aliyun.credentials.provider;

import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.http.MethodType;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.ParameterHelper;
import com.aliyun.credentials.utils.StringUtils;
import com.aliyun.tea.utils.Validate;
import com.google.gson.Gson;

import java.util.Map;

public class RamRoleArnCredentialProvider extends SessionCredentialsProvider {

    /**
     * Default duration for started sessions. Unit of Second
     */
    public int durationSeconds = 3600;
    /**
     * The arn of the role to be assumed.
     */
    private String roleArn;
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

    private String externalId;

    @Deprecated
    public RamRoleArnCredentialProvider(Configuration config) {
        this(config.getAccessKeyId(), config.getAccessKeySecret(), config.getRoleArn());
        this.roleSessionName = config.getRoleSessionName();
        this.connectTimeout = config.getConnectTimeout();
        this.readTimeout = config.getReadTimeout();
        if (!StringUtils.isEmpty(config.getSTSEndpoint())) {
            this.STSEndpoint = config.getSTSEndpoint();
        }
    }

    @Deprecated
    public RamRoleArnCredentialProvider(Config config) {
        this(config.accessKeyId, config.accessKeySecret, config.roleArn);
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
    public RamRoleArnCredentialProvider(String accessKeyId, String accessKeySecret, String roleArn) {
        super(new BuilderImpl());
        this.roleArn = roleArn;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
    }

    @Deprecated
    public RamRoleArnCredentialProvider(String accessKeyId, String accessKeySecret, String roleSessionName,
                                        String roleArn, String regionId, String policy) {
        this(accessKeyId, accessKeySecret, roleArn);
        this.roleSessionName = roleSessionName;
        this.regionId = regionId;
        this.policy = policy;
    }

    private RamRoleArnCredentialProvider(BuilderImpl builder) {
        super(builder);
        this.roleSessionName = builder.roleSessionName;
        this.durationSeconds = builder.durationSeconds;
        this.roleArn = builder.roleArn;
        this.regionId = builder.regionId;
        this.policy = builder.policy;
        this.connectTimeout = builder.connectionTimeout;
        this.readTimeout = builder.readTimeout;
        this.STSEndpoint = builder.STSEndpoint;
        this.accessKeyId = Validate.notNull(builder.accessKeyId, "AccessKeyId must not be null.");
        this.accessKeySecret = Validate.notNull(builder.accessKeySecret, "AccessKeySecret must not be null.");
        this.externalId = builder.externalId;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public RefreshResult<CredentialModel> refreshCredentials() {
        CompatibleUrlConnClient client = new CompatibleUrlConnClient();
        return createCredential(client);
    }

    public RefreshResult<CredentialModel> createCredential(CompatibleUrlConnClient client) {
        try {
            return getNewSessionCredentials(client);
        } catch (Exception e) {
            throw new CredentialException(e.getMessage(), e);
        } finally {
            client.close();
        }
    }

    @SuppressWarnings("unchecked")
    public RefreshResult<CredentialModel> getNewSessionCredentials(CompatibleUrlConnClient client) {
        ParameterHelper parameterHelper = new ParameterHelper();
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setUrlParameter("Action", "AssumeRole");
        httpRequest.setUrlParameter("Format", "JSON");
        httpRequest.setUrlParameter("Version", "2015-04-01");
        httpRequest.setUrlParameter("DurationSeconds", String.valueOf(durationSeconds));
        httpRequest.setUrlParameter("RoleArn", this.roleArn);
        httpRequest.setUrlParameter("AccessKeyId", this.accessKeyId);
        httpRequest.setUrlParameter("RoleSessionName", this.roleSessionName);
        if (policy != null) {
            httpRequest.setUrlParameter("Policy", this.policy);
        }
        if (externalId != null) {
            httpRequest.setUrlParameter("ExternalId", this.externalId);
        }
        httpRequest.setSysMethod(MethodType.GET);
        httpRequest.setSysConnectTimeout(this.connectTimeout);
        httpRequest.setSysReadTimeout(this.readTimeout);
        String strToSign = parameterHelper.composeStringToSign(MethodType.GET, httpRequest.getUrlParameters());
        String signature = parameterHelper.signString(strToSign, this.accessKeySecret + "&");
        httpRequest.setUrlParameter("Signature", signature);
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
            CredentialModel credential = CredentialModel.builder()
                    .accessKeyId(result.get("AccessKeyId"))
                    .accessKeySecret(result.get("AccessKeySecret"))
                    .securityToken(result.get("SecurityToken"))
                    .type(AuthConstant.RAM_ROLE_ARN)
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

    public String getRoleSessionName() {
        return roleSessionName;
    }

    public void setRoleSessionName(String roleSessionName) {
        this.roleSessionName = roleSessionName;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

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

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
    public String getExternalId() {
        return this.externalId;
    }

    public interface Builder extends SessionCredentialsProvider.Builder<RamRoleArnCredentialProvider, Builder> {
        Builder roleSessionName(String roleSessionName);

        Builder durationSeconds(int durationSeconds);

        Builder roleArn(String roleArn);

        Builder regionId(String regionId);

        Builder policy(String policy);

        Builder connectionTimeout(int connectionTimeout);

        Builder readTimeout(int readTimeout);

        Builder STSEndpoint(String STSEndpoint);

        Builder accessKeyId(String accessKeyId);

        Builder accessKeySecret(String accessKeySecret);

        Builder externalId(String externalId);

        @Override
        RamRoleArnCredentialProvider build();
    }

    private static final class BuilderImpl
            extends SessionCredentialsProvider.BuilderImpl<RamRoleArnCredentialProvider, Builder>
            implements Builder {
        private String roleSessionName = StringUtils.isEmpty(System.getenv("ALIBABA_CLOUD_ROLE_SESSION_NAME")) ?
                "defaultSessionName"
                : System.getenv("ALIBABA_CLOUD_ROLE_SESSION_NAME");
        private int durationSeconds = 3600;
        private String roleArn = System.getenv("ALIBABA_CLOUD_ROLE_ARN");
        private String regionId = "cn-hangzhou";
        private String policy;
        private int connectionTimeout = 1000;
        private int readTimeout = 1000;
        private String STSEndpoint = "sts.aliyuncs.com";
        private String accessKeyId;
        private String accessKeySecret;
        private String externalId;

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

        public Builder accessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        public Builder accessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
            return this;
        }

        public Builder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        @Override
        public RamRoleArnCredentialProvider build() {
            return new RamRoleArnCredentialProvider(this);
        }
    }
}