package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.tea.utils.Validate;

import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.SECONDS;

public class RefreshCachedSupplier<T> implements AutoCloseable {
    /**
     * Maximum time to wait for a blocking refresh lock before calling refresh again. Unit of milliseconds.
     */
    private static final long REFRESH_BLOCKING_MAX_WAIT = 5 * 1000;
    private final Lock refreshLock = new ReentrantLock();
    private final AtomicBoolean asyncRefreshing = new AtomicBoolean(false);
    private final ExecutorService executor = new ThreadPoolExecutor(0, 1, 5, SECONDS,
            new LinkedBlockingQueue<Runnable>(1),
            Executors.defaultThreadFactory());
    private volatile RefreshResult<T> cachedValue = RefreshResult.builder((T) null)
            .staleTime(0)
            .build();

    private final Callable<RefreshResult<T>> refreshCallable;
    private final boolean asyncUpdateEnabled;

    private RefreshCachedSupplier(Builder<T> builder) {
        this.refreshCallable = Validate.notNull(builder.refreshCallable, "Refresh Callable is null.");
        this.asyncUpdateEnabled = builder.asyncUpdateEnabled;
    }

    public static <T> Builder<T> builder(Callable<RefreshResult<T>> refreshCallable) {
        return new Builder<T>(refreshCallable);
    }

    public T get() {
        if (cacheIsStale()) {
            if (this.asyncUpdateEnabled) {
                asyncRefresh();
            } else {
                blockingRefresh();
            }
        }
        return null != this.cachedValue ? this.cachedValue.value() : null;
    }

    private void refreshCache() {
        try {
            this.cachedValue = refreshCallable.call();
        } catch (CredentialException ex) {
            throw ex;
        } catch (Exception e) {
            throw new CredentialException("Failed to refresh credentials.", e);
        }
    }

    private void blockingRefresh() {
        try {
            if (refreshLock
                    .tryLock(REFRESH_BLOCKING_MAX_WAIT, TimeUnit.MILLISECONDS)) {
                try {
                    if (cacheIsStale()) {
                        refreshCache();
                    }
                } finally {
                    refreshLock.unlock();
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting to refresh the value.", ex);
        } catch (Exception ex) {
            Thread.currentThread().interrupt();
            throw ex;
        }
    }

    /**
     * Used to asynchronously refresh the value. Caller is never blocked.
     */
    private void asyncRefresh() {
        // Immediately return if refresh already in progress
        if (asyncRefreshing.compareAndSet(false, true)) {
            try {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            refreshCache();
                        } finally {
                            asyncRefreshing.set(false);
                        }
                    }
                });
            } catch (RuntimeException ex) {
                asyncRefreshing.set(false);
                throw ex;
            }
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private boolean cacheIsStale() {
        return this.cachedValue == null || new Date().getTime() >= this.cachedValue.staleTime();
    }

    public static final class Builder<T> {
        private final Callable<RefreshResult<T>> refreshCallable;
        private boolean asyncUpdateEnabled;

        private Builder(Callable<RefreshResult<T>> refreshCallable) {
            this.refreshCallable = refreshCallable;
        }

        public Builder<T> asyncUpdateEnabled(Boolean asyncUpdateEnabled) {
            this.asyncUpdateEnabled = asyncUpdateEnabled;
            return this;
        }

        public RefreshCachedSupplier<T> build() {
            return new RefreshCachedSupplier<T>(this);
        }
    }

}
