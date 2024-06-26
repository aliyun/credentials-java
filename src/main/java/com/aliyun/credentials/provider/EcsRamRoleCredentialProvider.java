package com.aliyun.credentials.provider;

import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthUtils;
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
        this.fetcher = new ECSMetadataServiceCredentialsFetcher(roleName,
                builder.disableIMDSv1,
                builder.metadataTokenDuration,
                builder.connectionTimeout,
                builder.readTimeout);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public RefreshResult<CredentialModel> refreshCredentials() {
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
        Builder roleName(String roleName);

        Builder disableIMDSv1(boolean disableIMDSv1);

        Builder enableIMDSv2(boolean enableIMDSv2);

        Builder metadataTokenDuration(int metadataTokenDuration);

        Builder connectionTimeout(int connectionTimeout);

        Builder readTimeout(int readTimeout);

        @Override
        EcsRamRoleCredentialProvider build();
    }

    private static final class BuilderImpl
            extends SessionCredentialsProvider.BuilderImpl<EcsRamRoleCredentialProvider, Builder>
            implements Builder {
        private String roleName;
        private boolean disableIMDSv1 = AuthUtils.getDisableECSIMDSv1();
        private boolean enableIMDSv2 = AuthUtils.getEnableECSIMDSv2();
        private int metadataTokenDuration = 21600;
        private int connectionTimeout = 1000;
        private int readTimeout = 1000;

        public Builder roleName(String roleName) {
            this.roleName = roleName;
            return this;
        }

        public Builder disableIMDSv1(boolean disableIMDSv1) {
            this.disableIMDSv1 = disableIMDSv1;
            return this;
        }

        public Builder enableIMDSv2(boolean enableIMDSv2) {
            this.enableIMDSv2 = enableIMDSv2;
            return this;
        }

        public Builder metadataTokenDuration(int metadataTokenDuration) {
            if (metadataTokenDuration > 0) {
                this.metadataTokenDuration = metadataTokenDuration;
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

        @Override
        public EcsRamRoleCredentialProvider build() {
            return new EcsRamRoleCredentialProvider(this);
        }
    }
}