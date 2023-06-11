package com.aliyun.credentials.provider;

import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.models.Credential;
import com.aliyun.credentials.utils.StringUtils;

public class EcsRamRoleCredentialProvider extends SessionCredentialsProvider {

    private ECSMetadataServiceCredentialsFetcher fetcher;

    @Deprecated
    public EcsRamRoleCredentialProvider(String roleName) {
        super(new BuilderImpl());
        if (StringUtils.isEmpty(roleName)) {
            CompatibleUrlConnClient client = new CompatibleUrlConnClient();
            roleName = new ECSMetadataServiceCredentialsFetcher("").fetchRoleName(client);
        }
        this.fetcher = new ECSMetadataServiceCredentialsFetcher(roleName);
    }

    @Deprecated
    public EcsRamRoleCredentialProvider(Configuration config) {
        super(new BuilderImpl());
        if (StringUtils.isEmpty(config.getRoleName())) {
            CompatibleUrlConnClient client = new CompatibleUrlConnClient();
            String roleName = new ECSMetadataServiceCredentialsFetcher("").fetchRoleName(client);
            config.setRoleName(roleName);
        }
        this.fetcher = new ECSMetadataServiceCredentialsFetcher(config.getRoleName(), config.getConnectTimeout(), config.getReadTimeout());
    }

    @Deprecated
    public EcsRamRoleCredentialProvider(Config config) {
        super(new BuilderImpl());
        if (StringUtils.isEmpty(config.roleName)) {
            CompatibleUrlConnClient client = new CompatibleUrlConnClient();
            String roleName = new ECSMetadataServiceCredentialsFetcher("").fetchRoleName(client);
            config.roleName = roleName;
        }
        this.fetcher = new ECSMetadataServiceCredentialsFetcher(config.roleName, config.connectTimeout, config.timeout);
    }

    private EcsRamRoleCredentialProvider(BuilderImpl builder) {
        super(builder);
        String roleName = builder.roleName;
        if (StringUtils.isEmpty(roleName)) {
            CompatibleUrlConnClient client = new CompatibleUrlConnClient();
            roleName = new ECSMetadataServiceCredentialsFetcher("").fetchRoleName(client);
        }
        this.fetcher = new ECSMetadataServiceCredentialsFetcher(roleName, builder.connectionTimeout, builder.readTimeout);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public RefreshResult<Credential> refreshCredentials() {
        CompatibleUrlConnClient client = new CompatibleUrlConnClient();
        return fetcher.fetch(client);
    }

    public ECSMetadataServiceCredentialsFetcher getFetcher() {
        return fetcher;
    }

    public void setFetcher(ECSMetadataServiceCredentialsFetcher fetcher) {
        this.fetcher = fetcher;
    }

    public interface Builder extends SessionCredentialsProvider.Builder<EcsRamRoleCredentialProvider, Builder> {
        Builder roleName(String roleSessionName);

        Builder connectionTimeout(int connectionTimeout);

        Builder readTimeout(int readTimeout);

        @Override
        EcsRamRoleCredentialProvider build();
    }

    private static final class BuilderImpl
            extends SessionCredentialsProvider.BuilderImpl<EcsRamRoleCredentialProvider, Builder>
            implements Builder {
        private String roleName;
        private int connectionTimeout = 1000;
        private int readTimeout = 1000;

        public Builder roleName(String roleName) {
            this.roleName = roleName;
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

        @Override
        public EcsRamRoleCredentialProvider build() {
            return new EcsRamRoleCredentialProvider(this);
        }
    }
}