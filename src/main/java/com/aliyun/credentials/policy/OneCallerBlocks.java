package com.aliyun.credentials.policy;

import java.util.concurrent.atomic.AtomicBoolean;

public class OneCallerBlocks implements PrefetchStrategy {
    private final AtomicBoolean currentlyRefreshing = new AtomicBoolean(false);

    @Override
    public void prefetch(Runnable valueUpdater) {
        if (currentlyRefreshing.compareAndSet(false, true)) {
            try {
                valueUpdater.run();
            } finally {
                currentlyRefreshing.set(false);
            }
        }
    }
}
