package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.policy.NonBlocking;
import com.aliyun.credentials.policy.OneCallerBlocks;
import com.aliyun.credentials.policy.PrefetchStrategy;
import com.aliyun.credentials.utils.ParameterHelper;
import com.aliyun.tea.logging.ClientLogger;
import com.aliyun.tea.utils.Validate;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RefreshCachedSupplier<T> implements AutoCloseable {
    private static final ClientLogger logger = new ClientLogger(RefreshCachedSupplier.class);
    static final long STALE_TIME = 15 * 60 * 1000;
    /**
     * Maximum time to wait for a blocking refresh lock before calling refresh again. Unit of milliseconds.
     */
    private static final long REFRESH_BLOCKING_MAX_WAIT = 5 * 1000;
    private final Lock refreshLock = new ReentrantLock();
    final PrefetchStrategy prefetchStrategy;
    private final AtomicInteger consecutiveRefreshFailures = new AtomicInteger(0);
    final StaleValueBehavior staleValueBehavior;
    private static final Random JITTER = new Random();

    private volatile RefreshResult<T> cachedValue;

    private final Callable<RefreshResult<T>> refreshCallable;

    private RefreshCachedSupplier(Builder<T> builder) {
        this.staleValueBehavior = Validate.notNull(builder.staleValueBehavior, "StaleValueBehavior is null.");
        Validate.notNull(builder.jitterEnabled, "JitterEnabled is null.");
        this.refreshCallable = Validate.notNull(builder.refreshCallable, "Refresh Callable is null.");
        if (builder.asyncUpdateEnabled) {
            prefetchStrategy = new NonBlocking();
        } else {
            prefetchStrategy = new OneCallerBlocks();
        }
    }

    public static <T> Builder<T> builder(Callable<RefreshResult<T>> refreshCallable) {
        return new Builder<T>(refreshCallable);
    }

    public T get() {
        if (cacheIsStale()) {
            logger.verbose("Refreshing credentials synchronously");
            refreshCache();
        } else if (shouldInitiateCachePrefetch()) {
            logger.verbose("Prefetching credentials, using prefetch strategy: {}", prefetchStrategy.toString());
            prefetchCache();
        } else {
            logger.verbose("get local credentials");
        }
        return this.cachedValue.value();
    }

    private void prefetchCache() {
        prefetchStrategy.prefetch(this::refreshCache);
    }

    private void refreshCache() {
        try {
            boolean lockAcquired = refreshLock.tryLock(REFRESH_BLOCKING_MAX_WAIT, TimeUnit.MILLISECONDS);
            try {
                if (cacheIsStale() || shouldInitiateCachePrefetch()) {
                    try {
                        this.cachedValue = handleFetchedSuccess(refreshCallable.call());
                    } catch (Exception ex) {
                        this.cachedValue = handleFetchedFailure(ex);
                    }
                }
            } finally {
                if (lockAcquired) {
                    refreshLock.unlock();
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting to refresh the value.", ex);
        } catch (CredentialException ex) {
            throw ex;
        } catch (Exception e) {
            throw new CredentialException("Failed to refresh credentials.", e);
        }
    }

    @Override
    public void close() {
        prefetchStrategy.close();
    }

    private boolean cacheIsStale() {
        return this.cachedValue == null || new Date().getTime() >= this.cachedValue.staleTime();
    }

    private boolean shouldInitiateCachePrefetch() {
        return this.cachedValue == null || new Date().getTime() >= this.cachedValue.prefetchTime();
    }

    RefreshResult<T> handleFetchedSuccess(RefreshResult<T> value) {
        logger.verbose("Refresh credentials successfully, retrieved value is {}, cached value is {}", value, cachedValue);
        consecutiveRefreshFailures.set(0);
        long now = new Date().getTime();
        // 过期时间大于15分钟，不用管
        if (now < value.staleTime()) {
            logger.verbose("Retrieved value stale time is {}. Using staleTime "
                    + "of {}", ParameterHelper.getTimeString(value.staleTime()), ParameterHelper.getTimeString(value.staleTime()));
            return value;
        }
        // 不足或等于15分钟，但未过期，下次会再次刷新
        if (now < value.staleTime() + STALE_TIME) {
            logger.warning("Retrieved value stale time is in the past ({}). Using staleTime "
                    + "of {}", ParameterHelper.getTimeString(value.staleTime()), ParameterHelper.getTimeString(now));
            return value.toBuilder().staleTime(now).build();
        }
        logger.warning("Retrieved value expiration time of the credential is in the past ({}). Trying use the cached value.", ParameterHelper.getTimeString(value.staleTime() + STALE_TIME));
        // 已过期，看缓存，缓存若大于15分钟，返回缓存，若小于15分钟，则根据策略判断是立刻重试还是稍后重试
        if (null == this.cachedValue) {
            throw new CredentialException("No cached value was found.");
        } else if (now < this.cachedValue.staleTime()) {
            logger.warning("Cached value staleTime is {}. Using staleTime of {}", ParameterHelper.getTimeString(this.cachedValue.staleTime()), ParameterHelper.getTimeString(this.cachedValue.staleTime()));
            return cachedValue;
        } else {
            switch (staleValueBehavior) {
                case STRICT:
                    // 立马重试
                    logger.warning("Cached value expiration is in the past (" + this.cachedValue.staleTime() + "). Using expiration "
                            + "of " + (now + 1000));
                    return cachedValue.toBuilder().staleTime(now + 1000).build();
                case ALLOW:
                    // 一分钟左右重试一次
                    long waitUntilNextRefresh = 50 * 1000 + JITTER.nextInt(20 * 1000 + 1);
                    long nextRefreshTime = now + waitUntilNextRefresh;
                    logger.warning("Cached value expiration has been extended to " + nextRefreshTime + " because the downstream "
                            + "service returned a time in the past: " + value.staleTime());
                    return cachedValue.toBuilder()
                            .staleTime(nextRefreshTime)
                            .build();
                default:
                    throw new IllegalStateException("Unknown stale-value-behavior: " + staleValueBehavior);
            }

        }
    }

    RefreshResult<T> handleFetchedFailure(Exception exception) throws Exception {
        logger.warning("Refresh credentials failed, cached value is {}, error: {}", cachedValue, exception.getMessage());
        RefreshResult<T> currentCachedValue = cachedValue;
        if (currentCachedValue == null) {
            throw logger.logThrowableAsError(exception);
        }
        long now = new Date().getTime();
        if (now < currentCachedValue.staleTime()) {
            return currentCachedValue;
        }
        int numFailures = consecutiveRefreshFailures.incrementAndGet();
        switch (staleValueBehavior) {
            case STRICT:
                throw logger.logThrowableAsError(exception);
            case ALLOW:
                // 采用退避算法，立刻重试
                long newStaleTime = jitterTime(now, 1000, maxStaleFailureJitter(numFailures));
                logger.warning("Cached value expiration has been extended to " + newStaleTime + " because calling the "
                        + "downstream service failed (consecutive failures: " + numFailures + ").");

                return currentCachedValue.toBuilder()
                        .staleTime(newStaleTime)
                        .build();
            default:
                throw new IllegalStateException("Unknown stale-value-behavior: " + staleValueBehavior);
        }
    }

    long jitterTime(long time, long jitterStart, long jitterEnd) {
        long jitterRange = jitterEnd - jitterStart;
        long jitterAmount = Math.abs(JITTER.nextLong() % jitterRange);
        return time + jitterStart + jitterAmount;
    }

    long maxStaleFailureJitter(int numFailures) {
        long exponentialBackoffMillis = (1L << numFailures - 1) * 100;
        return exponentialBackoffMillis > 10 * 1000 ? exponentialBackoffMillis : 10 * 1000;
    }

    public static final class Builder<T> {
        private final Callable<RefreshResult<T>> refreshCallable;
        private boolean asyncUpdateEnabled;
        private Boolean jitterEnabled = true;
        private StaleValueBehavior staleValueBehavior = StaleValueBehavior.STRICT;

        private Builder(Callable<RefreshResult<T>> refreshCallable) {
            this.refreshCallable = refreshCallable;
        }

        public Builder<T> asyncUpdateEnabled(Boolean asyncUpdateEnabled) {
            this.asyncUpdateEnabled = asyncUpdateEnabled;
            return this;
        }

        public Builder<T> staleValueBehavior(StaleValueBehavior staleValueBehavior) {
            this.staleValueBehavior = staleValueBehavior;
            return this;
        }

        Builder<T> jitterEnabled(Boolean jitterEnabled) {
            this.jitterEnabled = jitterEnabled;
            return this;
        }

        public RefreshCachedSupplier<T> build() {
            return new RefreshCachedSupplier<T>(this);
        }
    }

    public enum StaleValueBehavior {
        /**
         * Strictly treat the stale time. Never return a stale cached value (except when the supplier returns an expired
         * value, in which case the supplier will return the value but only for a very short period of time to prevent
         * overloading the underlying supplier).
         */
        STRICT,

        /**
         * Allow stale values to be returned from the cache. Value retrieval will never fail, as long as the cache has
         * succeeded when calling the underlying supplier at least once.
         */
        ALLOW
    }

}
