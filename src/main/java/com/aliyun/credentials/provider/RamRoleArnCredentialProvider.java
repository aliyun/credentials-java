package com.aliyun.credentials.provider;

import com.aliyun.credentials.api.ICredentials;
import com.aliyun.credentials.api.ICredentialsProvider;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.http.MethodType;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.*;
import com.aliyun.tea.utils.Validate;
import com.google.gson.Gson;

import java.util.Map;

import static com.aliyun.credentials.configure.Config.ENDPOINT_SUFFIX;
import static com.aliyun.credentials.configure.Config.STS_DEFAULT_ENDPOINT;

public class RamRoleArnCredentialProvider extends SessionCredentialsProvider {

    /**
     * Default duration for started sessions. Unit of Second
     */
    public int durationSeconds;
    /**
     * The arn of the role to be assumed.
     */
    private String roleArn;
    /**
     * An identifier for the assumed role session.
     */
    private String roleSessionName;

    private final ICredentialsProvider credentialsProvider;
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

    private String externalId;

    private RamRoleArnCredentialProvider(BuilderImpl builder) {
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

        this.policy = builder.policy;
        this.externalId = builder.externalId;
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

        if (null != builder.credentialsProvider) {
            this.credentialsProvider = builder.credentialsProvider;
        } else if (!StringUtils.isEmpty(builder.securityToken)) {
            this.credentialsProvider = StaticCredentialsProvider.builder()
                    .credential(CredentialModel.builder()
                            .accessKeyId(Validate.notNull(
                                    builder.accessKeyId, "AccessKeyId must not be null."))
                            .accessKeySecret(Validate.notNull(
                                    builder.accessKeySecret, "AccessKeySecret must not be null."))
                            .securityToken(Validate.notNull(
                                    builder.securityToken, "SecurityToken must not be null."))
                            .type(AuthConstant.STS)
                            .providerName(ProviderName.STATIC_STS)
                            .build())
                    .build();
        } else {
            this.credentialsProvider = StaticCredentialsProvider.builder()
                    .credential(CredentialModel.builder()
                            .accessKeyId(Validate.notNull(
                                    builder.accessKeyId, "AccessKeyId must not be null."))
                            .accessKeySecret(Validate.notNull(
                                    builder.accessKeySecret, "AccessKeySecret must not be null."))
                            .type(AuthConstant.ACCESS_KEY)
                            .providerName(ProviderName.STATIC_AK)
                            .build())
                    .build();
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
        return getNewSessionCredentials(client);
    }

    public RefreshResult<CredentialModel> getNewSessionCredentials(CompatibleUrlConnClient client) {
        ParameterHelper parameterHelper = new ParameterHelper();
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setUrlParameter("Action", "AssumeRole");
        httpRequest.setUrlParameter("Format", "JSON");
        httpRequest.setUrlParameter("Version", "2015-04-01");
        httpRequest.setUrlParameter("DurationSeconds", String.valueOf(durationSeconds));
        httpRequest.setUrlParameter("RoleArn", this.roleArn);

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

        ICredentials credentials = this.credentialsProvider.getCredentials();
        Validate.notNull(credentials, "Unable to load original credentials from the providers in RAM role arn.");
        httpRequest.setUrlParameter("AccessKeyId", credentials.getAccessKeyId());
        if (!StringUtils.isEmpty(credentials.getSecurityToken())) {
            httpRequest.setUrlParameter("SecurityToken", credentials.getSecurityToken());
        }
        String strToSign = parameterHelper.composeStringToSign(MethodType.GET, httpRequest.getUrlParameters());
        String signature = parameterHelper.signString(strToSign, credentials.getAccessKeySecret() + "&");
        httpRequest.setUrlParameter("Signature", signature);

        httpRequest.setSysUrl(parameterHelper.composeUrl(this.stsEndpoint, httpRequest.getUrlParameters(),
                "https"));
        HttpResponse httpResponse;
        try {
            httpResponse = client.syncInvoke(httpRequest);
        } catch (Exception e) {
            throw new CredentialException("Failed to connect RamRoleArn Service: " + e);
        }
        if (httpResponse.getResponseCode() != 200) {
            throw new CredentialException(String.format("Error refreshing credentials from RamRoleArn, HttpCode: %s, result: %s.", httpResponse.getResponseCode(), httpResponse.getHttpContentString()));
        }
        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(httpResponse.getHttpContentString(), Map.class);
        if (null == map || !map.containsKey("Credentials")) {
            throw new CredentialException(String.format("Error retrieving credentials from RamRoleArn result: %s.", httpResponse.getHttpContentString()));
        }
        Map<String, String> result = (Map<String, String>) map.get("Credentials");
        if (!result.containsKey("AccessKeyId") || !result.containsKey("AccessKeySecret") || !result.containsKey("SecurityToken")) {
            throw new CredentialException(String.format("Error retrieving credentials from RamRoleArn result: %s.", httpResponse.getHttpContentString()));
        }
        long expiration = ParameterHelper.getUTCDate(result.get("Expiration")).getTime();
        CredentialModel credential = CredentialModel.builder()
                .accessKeyId(result.get("AccessKeyId"))
                .accessKeySecret(result.get("AccessKeySecret"))
                .securityToken(result.get("SecurityToken"))
                .type(AuthConstant.RAM_ROLE_ARN)
                .providerName(String.format("%s/%s", this.getProviderName(), credentials.getProviderName()))
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

    public String getRoleSessionName() {
        return roleSessionName;
    }

    public void setRoleSessionName(String roleSessionName) {
        this.roleSessionName = roleSessionName;
    }

    private ICredentialsProvider getCredentialsProvider() {
        return this.credentialsProvider;
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

    public void setSTSEndpoint(String STSEndpoint) {
        this.stsEndpoint = STSEndpoint;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getExternalId() {
        return this.externalId;
    }

    @Override
    public String getProviderName() {
        return ProviderName.RAM_ROLE_ARN;
    }

    @Override
    public void close() {
        super.close();
    }

    public interface Builder extends SessionCredentialsProvider.Builder<RamRoleArnCredentialProvider, Builder> {
        Builder roleSessionName(String roleSessionName);

        Builder durationSeconds(Integer durationSeconds);

        Builder roleArn(String roleArn);

        Builder policy(String policy);

        Builder connectionTimeout(Integer connectionTimeout);

        Builder readTimeout(Integer readTimeout);

        Builder stsEndpoint(String STSEndpoint);

        Builder stsRegionId(String stsRegionId);

        Builder enableVpc(Boolean enableVpc);

        Builder accessKeyId(String accessKeyId);

        Builder accessKeySecret(String accessKeySecret);

        Builder securityToken(String securityToken);

        Builder credentialsProvider(ICredentialsProvider credentialsProvider);

        Builder externalId(String externalId);

        @Override
        RamRoleArnCredentialProvider build();
    }

    private static final class BuilderImpl
            extends SessionCredentialsProvider.BuilderImpl<RamRoleArnCredentialProvider, Builder>
            implements Builder {
        private String roleSessionName;
        private Integer durationSeconds;
        private String roleArn;
        private String policy;
        private Integer connectionTimeout;
        private Integer readTimeout;
        private String stsEndpoint;
        private String stsRegionId;
        private Boolean enableVpc;
        private String accessKeyId;
        private String accessKeySecret;
        private String securityToken;
        private ICredentialsProvider credentialsProvider;
        private String externalId;

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

        public Builder accessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        public Builder accessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
            return this;
        }

        public Builder securityToken(String securityToken) {
            this.securityToken = securityToken;
            return this;
        }

        public Builder credentialsProvider(ICredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
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