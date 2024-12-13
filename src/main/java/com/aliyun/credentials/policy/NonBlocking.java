package com.aliyun.credentials.policy;

import com.aliyun.tea.logging.ClientLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;

public class NonBlocking implements PrefetchStrategy {
    private static final ClientLogger logger = new ClientLogger(NonBlocking.class);
    private final AtomicBoolean currentlyRefreshing = new AtomicBoolean(false);

    private final ExecutorService executor;

    public NonBlocking() {
        this.executor = new ThreadPoolExecutor(0, 1, 5, SECONDS,
                new LinkedBlockingQueue<>(1),
                Executors.defaultThreadFactory());
    }

    @Override
    public void prefetch(Runnable valueUpdater) {
        if (currentlyRefreshing.compareAndSet(false, true)) {
            try {
                executor.submit(() -> {
                    try {
                        valueUpdater.run();
                    } finally {
                        currentlyRefreshing.set(false);
                    }
                });
            } catch (RuntimeException e) {
                currentlyRefreshing.set(false);
                throw e;
            }
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
