package com.aliyun.credentials.policy;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;

public class OneCallerBlocksTest {

    @Test
    public void testPrefetchExecutes() throws InterruptedException {
        OneCallerBlocks strategy = new OneCallerBlocks();
        Runnable mockUpdater = mock(Runnable.class);

        strategy.prefetch(mockUpdater);
        verify(mockUpdater, times(1)).run();

        // Multiple Prefetch Call
        AtomicInteger updateCounter = new AtomicInteger(0);
        Runnable multiUpdater = updateCounter::incrementAndGet;
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                strategy.prefetch(multiUpdater);
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        Assert.assertTrue(updateCounter.get() <= 10);

        // Reentrant Call
        Runnable updater = () -> {
            strategy.prefetch(mockUpdater);
        };
        strategy.prefetch(updater);
        verify(mockUpdater, times(1)).run();

        // Exception Handle
        Runnable failingUpdater = mock(Runnable.class);
        doThrow(new RuntimeException("Test exception")).when(failingUpdater).run();

        try {
            strategy.prefetch(failingUpdater);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("Test exception", e.getMessage());
        }

        strategy.close();
    }
}
