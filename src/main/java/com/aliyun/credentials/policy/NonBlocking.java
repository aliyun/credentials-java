package com.aliyun.credentials.policy;

import com.aliyun.tea.logging.ClientLogger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;

public class NonBlocking implements PrefetchStrategy {
    private static final ClientLogger logger = new ClientLogger(NonBlocking.class);
    static final int MAX_CONCURRENT_REFRESHES = 100;
    private static final Semaphore CONCURRENT_REFRESH_LEASES = new Semaphore(MAX_CONCURRENT_REFRESHES);
    private final AtomicBoolean currentlyRefreshing = new AtomicBoolean(false);

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60, SECONDS,
            new SynchronousQueue<>(),
            new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setName("non-blocking-refresh");
                    t.setDaemon(true);
                    return t;
                }
            });

    @Override
    public void prefetch(Runnable valueUpdater) {
        if (currentlyRefreshing.compareAndSet(false, true)) {
            if (!CONCURRENT_REFRESH_LEASES.tryAcquire()) {
                logger.warning("Skipping a background refresh task because there are too many other tasks running.");
                currentlyRefreshing.set(false);
                return;
            }
            try {
                executor.submit(() -> {
                    try {
                        valueUpdater.run();
                    } catch (Throwable t) {
                        logger.logThrowableAsWarning(t);
                    } finally {
                        CONCURRENT_REFRESH_LEASES.release();
                        currentlyRefreshing.set(false);
                    }
                });
            } catch (Throwable t) {
                logger.logThrowableAsWarning(t);
                CONCURRENT_REFRESH_LEASES.release();
                currentlyRefreshing.set(false);
            }
        }
    }

    @Override
    public void close() {
    }
}
