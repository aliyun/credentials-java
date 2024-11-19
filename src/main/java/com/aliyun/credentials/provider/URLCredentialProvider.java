package com.aliyun.credentials.provider;

import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.http.HttpRequest;
import com.aliyun.credentials.http.HttpResponse;
import com.aliyun.credentials.http.MethodType;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.*;
import com.google.gson.Gson;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class URLCredentialProvider extends SessionCredentialsProvider {

    private final URL credentialsURI;
    /**
     * Unit of millisecond
     */
    private int connectTimeout = 5000;
    private int readTimeout = 10000;

    @Deprecated
    public URLCredentialProvider() {
        this(System.getenv("ALIBABA_CLOUD_CREDENTIALS_URI"));
    }

    @Deprecated
    public URLCredentialProvider(String credentialsURI) {
        super(new BuilderImpl());
        if (StringUtils.isEmpty(credentialsURI)) {
            throw new CredentialException("Credential URI cannot be null.");
        }
        try {
            this.credentialsURI = new URL(credentialsURI);
        } catch (MalformedURLException e) {
            throw new CredentialException("Credential URI is not valid.");
        }
    }

    @Deprecated
    public URLCredentialProvider(URL credentialsURI) {
        super(new BuilderImpl());
        if (credentialsURI == null) {
            throw new CredentialException("Credential URI cannot be null.");
        }
        this.credentialsURI = credentialsURI;
    }

    @Deprecated
    public URLCredentialProvider(Configuration config) {
        this(config.getCredentialsURI());
        this.connectTimeout = config.getConnectTimeout();
        this.readTimeout = config.getReadTimeout();
    }

    @Deprecated
    public URLCredentialProvider(Config config) {
        this(config.credentialsURI);
        this.connectTimeout = config.connectTimeout;
        this.readTimeout = config.timeout;
    }

    private URLCredentialProvider(BuilderImpl builder) {
        super(builder);
        String credentialsURI = builder.credentialsURI == null ? AuthUtils.getEnvironmentCredentialsURI() : builder.credentialsURI;
        if (StringUtils.isEmpty(credentialsURI)) {
            throw new IllegalArgumentException("Credential URI or environment variable ALIBABA_CLOUD_CREDENTIALS_URI cannot be empty.");
        }
        try {
            this.credentialsURI = new URL(credentialsURI);
        } catch (MalformedURLException e) {
            throw new CredentialException("Credential URI is not valid.");
        }
        this.connectTimeout = builder.connectionTimeout == null ? 5000 : builder.connectionTimeout;
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
        HttpRequest request = new HttpRequest(this.credentialsURI.toString());
        request.setSysMethod(MethodType.GET);
        request.setSysConnectTimeout(connectTimeout);
        request.setSysReadTimeout(readTimeout);
        HttpResponse response;

        try {
            response = client.syncInvoke(request);
        } catch (Exception e) {
            throw new CredentialException("Failed to connect Server: " + e.toString(), e);
        } finally {
            client.close();
        }

        if (response.getResponseCode() >= 300 || response.getResponseCode() < 200) {
            throw new CredentialException("Failed to get credentials from server: " + this.credentialsURI.toString()
                    + "\nHttpCode=" + response.getResponseCode()
                    + "\nHttpRAWContent=" + response.getHttpContentString());
        }

        Gson gson = new Gson();
        Map<String, String> map;
        try {
            map = gson.fromJson(response.getHttpContentString(), Map.class);
        } catch (Exception e) {
            throw new CredentialException("Failed to get credentials from server: " + this.credentialsURI.toString()
                    + "\nHttpCode=" + response.getResponseCode()
                    + "\nHttpRAWContent=" + response.getHttpContentString(), e);
        }
        if (map.containsKey("Code") && map.get("Code").equals("Success")) {
            long expiration = ParameterHelper.getUTCDate(map.get("Expiration")).getTime();
            CredentialModel credential = CredentialModel.builder()
                    .accessKeyId(map.get("AccessKeyId"))
                    .accessKeySecret(map.get("AccessKeySecret"))
                    .securityToken(map.get("SecurityToken"))
                    .type(AuthConstant.CREDENTIALS_URI)
                    .providerName(this.getProviderName())
                    .expiration(expiration)
                    .build();
            return RefreshResult.builder(credential)
                    .staleTime(getStaleTime(expiration))
                    .build();
        } else {
            throw new CredentialException(String.format("Error retrieving credentials from credentialsURI result: %s.", response.getHttpContentString()));
        }
    }

    public String getURL() {
        return credentialsURI.toString();
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

    @Override
    public String getProviderName() {
        return ProviderName.CREDENTIALS_URI;
    }

    @Override
    public void close() {
    }

    public interface Builder extends SessionCredentialsProvider.Builder<URLCredentialProvider, Builder> {
        Builder credentialsURI(URL credentialsURI);

        Builder credentialsURI(String credentialsURI);

        Builder connectionTimeout(Integer connectionTimeout);

        Builder readTimeout(Integer readTimeout);

        @Override
        URLCredentialProvider build();
    }

    private static final class BuilderImpl
            extends SessionCredentialsProvider.BuilderImpl<URLCredentialProvider, Builder>
            implements Builder {

        private String credentialsURI;
        private Integer connectionTimeout;
        private Integer readTimeout;

        public Builder credentialsURI(URL credentialsURI) {
            this.credentialsURI = credentialsURI.toString();
            return this;
        }

        public Builder credentialsURI(String credentialsURI) {
            this.credentialsURI = credentialsURI;
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

        @Override
        public URLCredentialProvider build() {
            return new URLCredentialProvider(this);
        }
    }
}