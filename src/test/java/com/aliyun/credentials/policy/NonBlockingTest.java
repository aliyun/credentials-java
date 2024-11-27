package com.aliyun.credentials.policy;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;

public class NonBlockingTest {

    @Test
    public void testPrefetchExecutes() throws InterruptedException {
        NonBlocking strategy = new NonBlocking();
        Runnable mockUpdater = mock(Runnable.class);

        strategy.prefetch(mockUpdater);

        // Allow some time for the async execution
        Thread.sleep(500);

        verify(mockUpdater, times(1)).run();  // Ensure that the updater is run once

        AtomicInteger updateCounter = new AtomicInteger(0);
        Runnable updater = updateCounter::incrementAndGet;

        // Exceed the semaphore limit
        for (int i = 0; i < 105; i++) {
            strategy.prefetch(updater);
        }

        // Allow some time for the async executions
        Thread.sleep(500);

        // Maximum allowable concurrent updates should be limited by the semaphore
        Assert.assertTrue(updateCounter.get() <= NonBlocking.MAX_CONCURRENT_REFRESHES);
        Runnable failingUpdater = mock(Runnable.class);
        doThrow(new RuntimeException("Test exception")).when(failingUpdater).run();
        try {
            strategy.prefetch(failingUpdater);
        } catch (Exception ignored) {
            Assert.fail();
        }
        Thread.sleep(500);

        // Close the executor
        strategy.close();
    }
}
