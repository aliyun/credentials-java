package com.aliyun.credentials.provider;

import com.aliyun.credentials.models.CredentialModel;

import java.util.Date;
import java.util.concurrent.Callable;

import static com.aliyun.credentials.provider.RefreshCachedSupplier.StaleValueBehavior.STRICT;

public abstract class SessionCredentialsProvider implements AlibabaCloudCredentialsProvider {
    private final boolean asyncCredentialUpdateEnabled;
    private RefreshCachedSupplier<CredentialModel> credentialsCache;
    private final Callable<RefreshResult<CredentialModel>> refreshCallable = new Callable<RefreshResult<CredentialModel>>() {
        @Override
        public RefreshResult<CredentialModel> call() throws Exception {
            return refreshCredentials();
        }
    };

    public abstract RefreshResult<CredentialModel> refreshCredentials();

    protected SessionCredentialsProvider(BuilderImpl<?, ?> builder) {
        this.asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        this.credentialsCache = RefreshCachedSupplier.builder(refreshCallable)
                .asyncUpdateEnabled(this.asyncCredentialUpdateEnabled)
                .staleValueBehavior(builder.staleValueBehavior)
                .jitterEnabled(builder.jitterEnabled)
                .build();
    }

    public long getStaleTime(long expiration) {
        return expiration <= 0 ?
                new Date().getTime() + 60 * 60 * 1000
                : expiration - RefreshCachedSupplier.STALE_TIME;
    }

    @Override
    public CredentialModel getCredentials() {
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
        boolean asyncCredentialUpdateEnabled = false;
        Boolean jitterEnabled = true;
        RefreshCachedSupplier.StaleValueBehavior staleValueBehavior = STRICT;

        protected BuilderImpl() {
        }

        @Override
        public BuilderT asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
            return (BuilderT) this;
        }

        BuilderT staleValueBehavior(RefreshCachedSupplier.StaleValueBehavior staleValueBehavior) {
            this.staleValueBehavior = staleValueBehavior;
            return (BuilderT) this;
        }

        BuilderT jitterEnabled(Boolean jitterEnabled) {
            this.jitterEnabled = jitterEnabled;
            return (BuilderT) this;
        }
    }
}
