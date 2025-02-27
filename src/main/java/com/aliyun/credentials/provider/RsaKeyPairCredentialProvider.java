package com.aliyun.credentials.provider;

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

public class RsaKeyPairCredentialProvider extends SessionCredentialsProvider {

    /**
     * Default duration for started sessions. Unit of Second
     */
    public int durationSeconds;

    private String publicKeyId;
    private String privateKey;

    /**
     * Unit of millisecond
     */
    private int connectTimeout;
    private int readTimeout;

    /**
     * Endpoint of RAM OpenAPI
     */
    private String stsEndpoint;

    private RsaKeyPairCredentialProvider(BuilderImpl builder) {
        super(builder);
        this.durationSeconds = builder.durationSeconds == null ? 3600 : builder.durationSeconds;
        if (this.durationSeconds < 900) {
            throw new IllegalArgumentException("Session duration should be in the range of 900s - max session duration.");
        }
        this.connectTimeout = builder.connectionTimeout == null ? 5000 : builder.connectionTimeout;
        this.readTimeout = builder.readTimeout == null ? 10000 : builder.readTimeout;
        this.publicKeyId = Validate.notNull(builder.publicKeyId, "PublicKeyId must not be null.");
        this.privateKey = Validate.notNull(builder.privateKey, "PrivateKey must not be null.");
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
        return getNewSessionCredentials(client);
    }

    public RefreshResult<CredentialModel> getNewSessionCredentials(CompatibleUrlConnClient client) {
        if (StringUtils.isEmpty(this.privateKey)) {
            throw new IllegalArgumentException("PrivateKey must not be empty.");
        }
        ParameterHelper parameterHelper = new ParameterHelper();
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setUrlParameter("Action", "GenerateSessionAccessKey");
        httpRequest.setUrlParameter("Format", "JSON");
        httpRequest.setUrlParameter("Version", "2015-04-01");
        httpRequest.setUrlParameter("DurationSeconds", String.valueOf(durationSeconds));
        httpRequest.setUrlParameter("AccessKeyId", this.publicKeyId);
        String strToSign = parameterHelper.composeStringToSign(MethodType.GET, httpRequest.getUrlParameters());
        String signature = parameterHelper.signString(strToSign, this.privateKey + "&");
        httpRequest.setUrlParameter("Signature", signature);
        httpRequest.setSysMethod(MethodType.GET);
        httpRequest.setSysConnectTimeout(this.connectTimeout);
        httpRequest.setSysReadTimeout(this.readTimeout);
        httpRequest.setSysUrl(parameterHelper.composeUrl(this.stsEndpoint, httpRequest.getUrlParameters(),
                "https"));
        HttpResponse httpResponse;
        try {
            httpResponse = client.syncInvoke(httpRequest);
        } catch (Exception e) {
            throw new CredentialException("Failed to connect RsaKeyPair Service: " + e);
        }
        if (httpResponse.getResponseCode() != 200) {
            throw new CredentialException(String.format("Error refreshing credentials from RsaKeyPair, HttpCode: %s, result: %s.", httpResponse.getResponseCode(), httpResponse.getHttpContentString()));
        }
        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(httpResponse.getHttpContentString(), Map.class);
        if (null == map || !map.containsKey("SessionAccessKey")) {
            throw new CredentialException(String.format("Error retrieving credentials from RsaKeyPair result: %s.", httpResponse.getHttpContentString()));
        }
        Map<String, String> result = (Map<String, String>) map.get("SessionAccessKey");
        long expiration = ParameterHelper.getUTCDate(result.get("Expiration")).getTime();
        CredentialModel credential = CredentialModel.builder()
                .accessKeyId(result.get("SessionAccessKeyId"))
                .accessKeySecret(result.get("SessionAccessKeySecret"))
                .type(AuthConstant.RSA_KEY_PAIR)
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

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public void setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
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
        return ProviderName.RSA_KEY_PAIR;
    }

    @Override
    public void close() {
        super.close();
    }

    public interface Builder extends SessionCredentialsProvider.Builder<RsaKeyPairCredentialProvider, Builder> {
        Builder durationSeconds(Integer durationSeconds);

        Builder connectionTimeout(Integer connectionTimeout);

        Builder readTimeout(Integer readTimeout);

        Builder stsEndpoint(String stsEndpoint);

        Builder stsRegionId(String stsRegionId);

        Builder enableVpc(Boolean enableVpc);

        Builder publicKeyId(String publicKeyId);

        Builder privateKeyFile(String privateKeyFile);

        Builder privateKey(String privateKey);

        @Override
        RsaKeyPairCredentialProvider build();
    }

    private static final class BuilderImpl
            extends SessionCredentialsProvider.BuilderImpl<RsaKeyPairCredentialProvider, Builder>
            implements Builder {
        private Integer durationSeconds;
        private Integer connectionTimeout;
        private Integer readTimeout;
        private String stsEndpoint;
        private String stsRegionId;
        private Boolean enableVpc;
        private String publicKeyId;
        private String privateKey;

        public Builder durationSeconds(Integer durationSeconds) {
            this.durationSeconds = durationSeconds;
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

        public Builder publicKeyId(String publicKeyId) {
            this.publicKeyId = publicKeyId;
            return this;
        }

        public Builder privateKeyFile(String privateKeyFile) {
            if (!StringUtils.isEmpty(privateKeyFile)) {
                this.privateKey = AuthUtils.getPrivateKey(privateKeyFile);
            }
            return this;
        }

        public Builder privateKey(String privateKey) {
            this.privateKey = privateKey;
            return this;
        }


        @Override
        public RsaKeyPairCredentialProvider build() {
            return new RsaKeyPairCredentialProvider(this);
        }
    }
}
