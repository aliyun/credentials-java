package com.aliyun.credentials.provider;

import com.aliyun.credentials.models.Credential;

import java.util.Date;
import java.util.concurrent.Callable;

public abstract class SessionCredentialsProvider implements AlibabaCloudCredentialsProvider, AutoCloseable {
    private final boolean asyncCredentialUpdateEnabled;
    private RefreshCachedSupplier<Credential> credentialsCache;
    private final Callable<RefreshResult<Credential>> refreshCallable = new Callable<RefreshResult<Credential>>() {
        @Override
        public RefreshResult<Credential> call() throws Exception {
            return refreshCredentials();
        }
    };

    public abstract RefreshResult<Credential> refreshCredentials();

    protected SessionCredentialsProvider(BuilderImpl<?, ?> builder) {
        this.asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        this.credentialsCache = RefreshCachedSupplier.builder(refreshCallable)
                .asyncUpdateEnabled(this.asyncCredentialUpdateEnabled)
                .build();
    }

    public long getStaleTime(long expiration) {
        return expiration <= 0 ?
                new Date().getTime() + 60 * 60 * 1000
                : expiration - 3 * 60 * 1000;
    }

    @Override
    public Credential getCredentials() {
        return credentialsCache.get();
    }

    public boolean isAsyncCredentialUpdateEnabled() {
        return asyncCredentialUpdateEnabled;
    }

    @Override
    public void close() {
        if (null != credentialsCache) {
            credentialsCache.close();
        }
    }

    public interface Builder<ProviderT extends SessionCredentialsProvider, BuilderT extends Builder> {
        BuilderT asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled);

        ProviderT build();
    }

    protected abstract static class BuilderImpl<ProviderT extends SessionCredentialsProvider, BuilderT extends Builder>
            implements Builder<ProviderT, BuilderT> {
        private boolean asyncCredentialUpdateEnabled = false;

        protected BuilderImpl() {
        }

        @Override
        public BuilderT asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
            return (BuilderT) this;
        }
    }
}
