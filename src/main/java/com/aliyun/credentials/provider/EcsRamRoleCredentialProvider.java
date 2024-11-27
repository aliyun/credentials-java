package com.aliyun.credentials.provider;

import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthUtils;
import com.aliyun.credentials.utils.ProviderName;
import com.aliyun.credentials.utils.StringUtils;
import com.aliyun.tea.logging.ClientLogger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.aliyun.credentials.provider.RefreshCachedSupplier.StaleValueBehavior.ALLOW;

public class EcsRamRoleCredentialProvider extends SessionCredentialsProvider {
    private static final ClientLogger logger = new ClientLogger(EcsRamRoleCredentialProvider.class);
    private static final int ASYNC_REFRESH_INTERVAL_TIME_MINUTES = 1;
    private ECSMetadataServiceCredentialsFetcher fetcher;
    private volatile ScheduledExecutorService executor;
    private volatile boolean shouldRefresh = false;

    @Deprecated
    public EcsRamRoleCredentialProvider(String roleName) {
        super(new BuilderImpl());
        if (StringUtils.isEmpty(roleName)) {
            try (CompatibleUrlConnClient client = new CompatibleUrlConnClient()) {
                roleName = new ECSMetadataServiceCredentialsFetcher("").fetchRoleName(client);
            }
        }
        this.fetcher = new ECSMetadataServiceCredentialsFetcher(roleName);
        checkCredentialsUpdateAsynchronously();
    }

    @Deprecated
    public EcsRamRoleCredentialProvider(Configuration config) {
        super(new BuilderImpl());
        if (StringUtils.isEmpty(config.getRoleName())) {
            try (CompatibleUrlConnClient client = new CompatibleUrlConnClient()) {
                String roleName = new ECSMetadataServiceCredentialsFetcher("").fetchRoleName(client);
                config.setRoleName(roleName);
            }
        }
        this.fetcher = new ECSMetadataServiceCredentialsFetcher(config.getRoleName(), config.getConnectTimeout(), config.getReadTimeout());
        checkCredentialsUpdateAsynchronously();
    }

    @Deprecated
    public EcsRamRoleCredentialProvider(Config config) {
        super(new BuilderImpl());
        String roleName = config.roleName;
        if (StringUtils.isEmpty(roleName)) {
            try (CompatibleUrlConnClient client = new CompatibleUrlConnClient()) {
                roleName = new ECSMetadataServiceCredentialsFetcher(
                        "",
                        config.disableIMDSv1,
                        config.connectTimeout,
                        config.timeout
                ).fetchRoleName(client);
            }

        }
        this.fetcher = new ECSMetadataServiceCredentialsFetcher(
                roleName,
                config.disableIMDSv1,
                config.connectTimeout,
                config.timeout);
        checkCredentialsUpdateAsynchronously();
    }

    private EcsRamRoleCredentialProvider(BuilderImpl builder) {
        super(builder);
        if (AuthUtils.isDisableECSMetaData()) {
            throw new CredentialException("IMDS credentials is disabled.");
        }
        String roleName = builder.roleName == null ? AuthUtils.getEnvironmentECSMetaData() : builder.roleName;
        boolean disableIMDSv1 = builder.disableIMDSv1 == null ? AuthUtils.getDisableECSIMDSv1() : builder.disableIMDSv1;
        this.fetcher = new ECSMetadataServiceCredentialsFetcher(
                roleName,
                disableIMDSv1,
                builder.connectionTimeout,
                builder.readTimeout);
        checkCredentialsUpdateAsynchronously();
    }

    private void checkCredentialsUpdateAsynchronously() {
        if (isAsyncCredentialUpdateEnabled()) {
            executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setName("imds-credentials-check-and-refresh");
                    t.setDaemon(true);
                    return t;
                }
            });
            executor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (shouldRefresh) {
                            logger.info("Begin checking or refreshing credentials asynchronously");
                            getCredentials();
                        }
                    } catch (Exception re) {
                        handleAsyncRefreshError(re);
                    }
                }

                private void handleAsyncRefreshError(Exception e) {
                    logger.warning("Failed when checking or refreshing credentials asynchronously, error: {}.", e.getMessage());
                }
            }, 0, ASYNC_REFRESH_INTERVAL_TIME_MINUTES, TimeUnit.MINUTES);
        }
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public RefreshResult<CredentialModel> refreshCredentials() {
        try (CompatibleUrlConnClient client = new CompatibleUrlConnClient()) {
            RefreshResult<CredentialModel> result = fetcher.fetch(client);
            shouldRefresh = true;
            return result;
        }
    }

    public ECSMetadataServiceCredentialsFetcher getFetcher() {
        return fetcher;
    }

    public void setFetcher(ECSMetadataServiceCredentialsFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public String getProviderName() {
        return ProviderName.ECS_RAM_ROLE;
    }

    @Override
    public void close() {
        super.close();
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public interface Builder extends SessionCredentialsProvider.Builder<EcsRamRoleCredentialProvider, Builder> {
        Builder roleName(String roleName);

        Builder disableIMDSv1(Boolean disableIMDSv1);

        @Deprecated
        Builder enableIMDSv2(boolean enableIMDSv2);

        @Deprecated
        Builder metadataTokenDuration(int metadataTokenDuration);

        Builder connectionTimeout(Integer connectionTimeout);

        Builder readTimeout(Integer readTimeout);

        @Override
        EcsRamRoleCredentialProvider build();
    }

    private static final class BuilderImpl
            extends SessionCredentialsProvider.BuilderImpl<EcsRamRoleCredentialProvider, Builder>
            implements Builder {
        private String roleName;
        private Boolean disableIMDSv1;
        private boolean enableIMDSv2;
        private int metadataTokenDuration;
        private Integer connectionTimeout;
        private Integer readTimeout;

        private BuilderImpl() {
            this.asyncCredentialUpdateEnabled = true;
            this.jitterEnabled = true;
            this.staleValueBehavior = ALLOW;
        }

        public Builder roleName(String roleName) {
            this.roleName = roleName;
            return this;
        }

        public Builder disableIMDSv1(Boolean disableIMDSv1) {
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

        public Builder connectionTimeout(Integer connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder readTimeout(Integer readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        @Override
        public EcsRamRoleCredentialProvider build() {
            return new EcsRamRoleCredentialProvider(this);
        }
    }
}