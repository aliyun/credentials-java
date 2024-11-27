package com.aliyun.credentials.policy;

@FunctionalInterface
public interface PrefetchStrategy extends AutoCloseable {
    void prefetch(Runnable valueUpdater);

    default void close() {
    }
}
