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
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.utils.ParameterHelper;
import com.aliyun.credentials.utils.StringUtils;
import com.aliyun.tea.utils.Validate;
import com.google.gson.Gson;

import java.util.Map;

public class RsaKeyPairCredentialProvider extends SessionCredentialsProvider {

    /**
     * Default duration for started sessions. Unit of Second
     */
    public int durationSeconds = 3600;

    private String publicKeyId;
    private String privateKey;
    private String privateKeyFile;
    private String regionId = "cn-hangzhou";

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
    public RsaKeyPairCredentialProvider(Configuration config) {
        this(config.getPublicKeyId(), config.getPrivateKeyFile());
        this.connectTimeout = config.getConnectTimeout();
        this.readTimeout = config.getReadTimeout();
        if (!StringUtils.isEmpty(config.getSTSEndpoint())) {
            this.STSEndpoint = config.getSTSEndpoint();
        }
    }

    @Deprecated
    public RsaKeyPairCredentialProvider(Config config) {
        this(config.publicKeyId, config.privateKeyFile);
        this.connectTimeout = config.connectTimeout;
        this.readTimeout = config.timeout;
        if (!StringUtils.isEmpty(config.STSEndpoint)) {
            this.STSEndpoint = config.STSEndpoint;
        }
    }

    @Deprecated
    public RsaKeyPairCredentialProvider(String publicKeyId, String privateKey) {
        super(new BuilderImpl());
        this.publicKeyId = publicKeyId;
        this.privateKey = privateKey;
    }

    private RsaKeyPairCredentialProvider(BuilderImpl builder) {
        super(builder);
        this.durationSeconds = builder.durationSeconds;
        this.regionId = builder.regionId;
        this.connectTimeout = builder.connectionTimeout;
        this.readTimeout = builder.readTimeout;
        this.STSEndpoint = builder.STSEndpoint;
        this.publicKeyId = Validate.notNull(builder.publicKeyId, "PublicKeyId must not be null.");
        this.privateKeyFile = Validate.notNull(builder.privateKeyFile, "privateKeyFile must not be null.");
    }

    public static Builder builder() {
        return new BuilderImpl();
    }


    @Override
    public RefreshResult<CredentialModel> refreshCredentials() {
        CompatibleUrlConnClient client = new CompatibleUrlConnClient();
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
        if (!StringUtils.isEmpty(this.privateKeyFile)) {
            this.privateKey = AuthUtils.getOIDCToken(this.privateKeyFile);
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
        httpRequest.setSysUrl(parameterHelper.composeUrl(this.STSEndpoint, httpRequest.getUrlParameters(),
                "https"));
        HttpResponse httpResponse = client.syncInvoke(httpRequest);
        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(httpResponse.getHttpContentString(), Map.class);
        Map<String, String> result = (Map<String, String>) map.get("SessionAccessKey");
        long expiration = ParameterHelper.getUTCDate(result.get("Expiration")).getTime();
        CredentialModel credential = CredentialModel.builder()
                .accessKeyId(result.get("SessionAccessKeyId"))
                .accessKeySecret(result.get("SessionAccessKeySecret"))
                .type(AuthConstant.RSA_KEY_PAIR)
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

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
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

    public interface Builder extends SessionCredentialsProvider.Builder<RsaKeyPairCredentialProvider, Builder> {
        Builder durationSeconds(int durationSeconds);

        Builder regionId(String regionId);

        Builder connectionTimeout(int connectionTimeout);

        Builder readTimeout(int readTimeout);

        Builder STSEndpoint(String STSEndpoint);

        Builder publicKeyId(String publicKeyId);

        Builder privateKeyFile(String privateKeyFile);

        @Override
        RsaKeyPairCredentialProvider build();
    }

    private static final class BuilderImpl
            extends SessionCredentialsProvider.BuilderImpl<RsaKeyPairCredentialProvider, Builder>
            implements Builder {

        private int durationSeconds = 3600;

        private String regionId = "cn-hangzhou";

        private int connectionTimeout = 1000;
        private int readTimeout = 1000;
        private String STSEndpoint = "sts.aliyuncs.com";
        private String publicKeyId;
        private String privateKeyFile;

        public Builder durationSeconds(int durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public Builder regionId(String regionId) {
            if (!StringUtils.isEmpty(regionId)) {
                this.regionId = regionId;
            }
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

        public Builder publicKeyId(String publicKeyId) {
            this.publicKeyId = publicKeyId;
            return this;
        }

        public Builder privateKeyFile(String privateKeyFile) {
            this.privateKeyFile = privateKeyFile;
            return this;
        }

        @Override
        public RsaKeyPairCredentialProvider build() {
            return new RsaKeyPairCredentialProvider(this);
        }
    }
}
