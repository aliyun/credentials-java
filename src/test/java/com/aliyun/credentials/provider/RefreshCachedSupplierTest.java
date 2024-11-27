package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.policy.NonBlocking;
import com.aliyun.credentials.policy.OneCallerBlocks;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RefreshCachedSupplierTest {
    private Callable<RefreshResult<CredentialModel>> refreshCallable;

    @Before
    public void setup() {
        refreshCallable = this::refreshCredentials;
    }

    private RefreshResult<CredentialModel> refreshCredentials() {
        CredentialModel credential = CredentialModel.builder()
                .accessKeyId("newAccessKey")
                .build();
        return RefreshResult.builder(credential)
                .staleTime(new Date().getTime() + 10 * 60 * 1000)
                .prefetchTime(new Date().getTime() - 10 * 60 * 1000)
                .build();
    }

    @Test
    public void getReturnsCachedValueWhenNotStale() {
        RefreshCachedSupplier<CredentialModel> supplier = RefreshCachedSupplier.builder(refreshCallable).build();
        CredentialModel initialValue = supplier.get();
        CredentialModel result = supplier.get();
        Assert.assertEquals(initialValue.getAccessKeyId(), result.getAccessKeyId());
    }

    @Test
    public void testPrefetchStrategy() {
        RefreshCachedSupplier<CredentialModel> supplier = RefreshCachedSupplier.builder(refreshCallable)
                .asyncUpdateEnabled(false)
                .build();

        CredentialModel result = supplier.get();
        Assert.assertEquals("newAccessKey", result.getAccessKeyId());

        Assert.assertTrue(supplier.prefetchStrategy instanceof OneCallerBlocks);

        supplier = RefreshCachedSupplier.builder(refreshCallable)
                .asyncUpdateEnabled(true)
                .build();

        Assert.assertTrue(supplier.prefetchStrategy instanceof NonBlocking);
    }

    @Test
    public void testStaleValueBehavior() {
        RefreshCachedSupplier<CredentialModel> supplier = RefreshCachedSupplier.builder(refreshCallable)
                .build();

        CredentialModel result = supplier.get();
        Assert.assertEquals("newAccessKey", result.getAccessKeyId());

        Assert.assertEquals(RefreshCachedSupplier.StaleValueBehavior.STRICT, supplier.staleValueBehavior);

        supplier = RefreshCachedSupplier.builder(refreshCallable)
                .staleValueBehavior(RefreshCachedSupplier.StaleValueBehavior.ALLOW)
                .build();

        Assert.assertEquals(RefreshCachedSupplier.StaleValueBehavior.ALLOW, supplier.staleValueBehavior);
    }

    @Test
    public void testRefreshCache() {
        AtomicInteger refreshCount = new AtomicInteger();
        Callable<RefreshResult<CredentialModel>> refresher = () -> {
            refreshCount.getAndIncrement();
            CredentialModel credential = CredentialModel.builder()
                    .accessKeyId("newAccessKey")
                    .build();
            return RefreshResult.builder(credential)
                    .staleTime(new Date().getTime() + 10 * 60 * 1000)
                    .prefetchTime(new Date().getTime() - 10 * 60 * 1000)
                    .build();
        };
        RefreshCachedSupplier<CredentialModel> supplier = RefreshCachedSupplier.builder(refresher).build();
        Assert.assertEquals(0, refreshCount.get());
        supplier.get();
        Assert.assertEquals(1, refreshCount.get());
        supplier.get();
        Assert.assertEquals(2, refreshCount.get());

        refresher = () -> {
            refreshCount.getAndIncrement();
            CredentialModel credential = CredentialModel.builder()
                    .accessKeyId("newAccessKey")
                    .build();
            return RefreshResult.builder(credential)
                    .staleTime(new Date().getTime() + 10 * 60 * 1000)
                    .build();
        };
        supplier = RefreshCachedSupplier.builder(refresher).build();
        supplier.get();
        Assert.assertEquals(3, refreshCount.get());
        supplier.get();
        Assert.assertEquals(3, refreshCount.get());

        refresher = () -> {
            refreshCount.getAndIncrement();
            if (refreshCount.get() > 5) {
                throw new CredentialException("refresh fail");
            }
            CredentialModel credential = CredentialModel.builder()
                    .accessKeyId("newAccessKey")
                    .build();
            return RefreshResult.builder(credential)
                    .staleTime(new Date().getTime() - 10 * 60 * 1000)
                    .build();
        };
        supplier = RefreshCachedSupplier.builder(refresher).build();
        supplier.get();
        Assert.assertEquals(4, refreshCount.get());
        supplier.get();
        Assert.assertEquals(5, refreshCount.get());
        try {
            supplier.get();
        } catch (CredentialException e) {
            Assert.assertEquals("refresh fail", e.getMessage());
        }

        refresher = () -> {
            refreshCount.getAndIncrement();
            if (refreshCount.get() > 7) {
                throw new CredentialException("refresh fail");
            }
            CredentialModel credential = CredentialModel.builder()
                    .accessKeyId("newAccessKey")
                    .build();
            return RefreshResult.builder(credential)
                    .staleTime(new Date().getTime() + 10 * 60 * 1000)
                    .prefetchTime(new Date().getTime() - 10 * 60 * 1000)
                    .build();
        };
        supplier = RefreshCachedSupplier.builder(refresher).build();
        supplier.get();
        Assert.assertEquals(7, refreshCount.get());
        // 测试不抛错，使用缓存值
        supplier.get();
        Assert.assertEquals(8, refreshCount.get());
    }

    @Test
    public void testHandleFetchedSuccess() throws Exception {
        RefreshCachedSupplier<CredentialModel> supplier = RefreshCachedSupplier.builder(refreshCallable).build();

        RefreshResult<CredentialModel> freshResult = RefreshResult.builder(CredentialModel.builder()
                        .accessKeyId("newAccessKey")
                        .build())
                .staleTime(new Date().getTime() + 10 * 60 * 1000)
                .build();

        RefreshResult<CredentialModel> updatedValue = supplier.handleFetchedSuccess(freshResult);
        Assert.assertSame(freshResult, updatedValue);

        freshResult = RefreshResult.builder(CredentialModel.builder()
                        .accessKeyId("newAccessKey")
                        .build())
                .staleTime(new Date().getTime() - 2 * 60 * 1000)
                .build();
        updatedValue = supplier.handleFetchedSuccess(freshResult);
        Assert.assertNotSame(freshResult, updatedValue);
        Assert.assertNotEquals(freshResult.staleTime(), updatedValue.staleTime());

        freshResult = RefreshResult.builder(CredentialModel.builder()
                        .accessKeyId("newAccessKey")
                        .build())
                .staleTime(new Date().getTime() - 20 * 60 * 1000)
                .build();

        try {
            supplier.handleFetchedSuccess(freshResult);
        } catch (CredentialException e) {
            Assert.assertEquals("No cached value was found.", e.getMessage());
        }

        RefreshResult<CredentialModel> cachedValue = RefreshResult.builder(CredentialModel.builder()
                        .accessKeyId("newAccessKey")
                        .build())
                .staleTime(new Date().getTime() + 10 * 60 * 1000)
                .build();
        setPrivateField(RefreshCachedSupplier.class, "cachedValue", supplier, cachedValue);
        updatedValue = supplier.handleFetchedSuccess(freshResult);
        Assert.assertSame(cachedValue, updatedValue);

        supplier = RefreshCachedSupplier.builder(refreshCallable)
                .staleValueBehavior(RefreshCachedSupplier.StaleValueBehavior.STRICT)
                .build();
        cachedValue = RefreshResult.builder(CredentialModel.builder()
                        .accessKeyId("newAccessKey")
                        .build())
                .staleTime(new Date().getTime() - 2 * 60 * 1000)
                .build();
        setPrivateField(RefreshCachedSupplier.class, "cachedValue", supplier, cachedValue);
        updatedValue = supplier.handleFetchedSuccess(freshResult);
        Assert.assertNotSame(cachedValue, updatedValue);

        supplier = RefreshCachedSupplier.builder(refreshCallable)
                .staleValueBehavior(RefreshCachedSupplier.StaleValueBehavior.ALLOW)
                .build();
        setPrivateField(RefreshCachedSupplier.class, "cachedValue", supplier, cachedValue);
        updatedValue = supplier.handleFetchedSuccess(freshResult);
        Assert.assertNotSame(cachedValue, updatedValue);
    }

    @Test
    public void testHandleFetchedFailure() throws Exception {
        RefreshCachedSupplier<CredentialModel> supplier = RefreshCachedSupplier.builder(refreshCallable).build();

        RefreshCachedSupplier<CredentialModel> finalSupplier = supplier;
        CredentialException exception = Assert.assertThrows(CredentialException.class, () -> {
            finalSupplier.handleFetchedFailure(new CredentialException("exception for test"));
        });

        Assert.assertEquals("exception for test", exception.getMessage());

        RefreshResult<CredentialModel> cachedValue = RefreshResult.builder(CredentialModel.builder()
                        .accessKeyId("newAccessKey")
                        .build())
                .staleTime(new Date().getTime() + 10 * 60 * 1000)
                .build();
        setPrivateField(RefreshCachedSupplier.class, "cachedValue", supplier, cachedValue);
        RefreshResult<CredentialModel> updatedValue = supplier.handleFetchedFailure(new CredentialException("exception for test"));
        Assert.assertSame(cachedValue, updatedValue);

        supplier = RefreshCachedSupplier.builder(refreshCallable)
                .staleValueBehavior(RefreshCachedSupplier.StaleValueBehavior.STRICT)
                .build();
        cachedValue = RefreshResult.builder(CredentialModel.builder()
                        .accessKeyId("newAccessKey")
                        .build())
                .staleTime(new Date().getTime() - 2 * 60 * 1000)
                .build();
        setPrivateField(RefreshCachedSupplier.class, "cachedValue", supplier, cachedValue);
        RefreshCachedSupplier<CredentialModel> finalSupplier1 = supplier;
        exception = Assert.assertThrows(CredentialException.class, () -> {
            finalSupplier1.handleFetchedFailure(new CredentialException("exception for test"));
        });
        Assert.assertEquals("exception for test", exception.getMessage());

        supplier = RefreshCachedSupplier.builder(refreshCallable)
                .staleValueBehavior(RefreshCachedSupplier.StaleValueBehavior.ALLOW)
                .build();
        setPrivateField(RefreshCachedSupplier.class, "cachedValue", supplier, cachedValue);
        updatedValue = supplier.handleFetchedFailure(new CredentialException("exception for test"));
        Assert.assertNotSame(cachedValue, updatedValue);
    }

    @Test
    public void testJitterTime() {
        RefreshCachedSupplier<CredentialModel> supplier = RefreshCachedSupplier.builder(refreshCallable).build();

        long jitteredTime = supplier.jitterTime(1735627102627L, 1000L, 10000L);
        Assert.assertTrue(jitteredTime > 1735627102627L && jitteredTime < 1735627112627L);

        Assert.assertEquals(10000L, supplier.maxStaleFailureJitter(2));
        Assert.assertEquals(51200L, supplier.maxStaleFailureJitter(10));
    }

    private void setPrivateField(Class<?> clazz, String fieldName, Object objInstance, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(objInstance, value);
    }

}
