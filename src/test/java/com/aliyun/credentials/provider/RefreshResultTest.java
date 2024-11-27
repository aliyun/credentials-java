package com.aliyun.credentials.provider;

import org.junit.Test;

import static org.junit.Assert.*;

public class RefreshResultTest {
    @Test
    public void testBuilderAndBuild() {
        String testValue = "testValue";
        long staleTime = 1000L;
        long prefetchTime = 500L;
        RefreshResult<String> result = RefreshResult.builder(testValue).staleTime(staleTime).prefetchTime(prefetchTime).build();

        assertNotNull(result);
        assertEquals(testValue, result.value());
        assertEquals(staleTime, result.staleTime());
        assertEquals(prefetchTime, result.prefetchTime());
    }

    @Test
    public void testDefaultValuesInBuilder() {
        String testValue = "defaultValue";
        RefreshResult<String> result = RefreshResult.builder(testValue).build();

        assertNotNull(result);
        assertEquals(testValue, result.value());
        assertEquals(Long.MAX_VALUE, result.staleTime());
        assertEquals(Long.MAX_VALUE, result.prefetchTime());
    }

    @Test
    public void testToString() {
        String testValue = "toStringTest";
        long staleTime = 2000L;
        long prefetchTime = 1000L;

        RefreshResult<String> result = RefreshResult.builder(testValue).staleTime(staleTime).prefetchTime(prefetchTime).build();

        String expected = String.format("RefreshResult(value=%s, staleTime=%d, prefetchTime=%d)", testValue, staleTime, prefetchTime);
        assertEquals(expected, result.toString());
    }

    @Test
    public void testToBuilder() {
        String testValue = "builderTest";
        long staleTime = 3000L;
        long prefetchTime = 1500L;

        RefreshResult<String> original = RefreshResult.builder(testValue).staleTime(staleTime).prefetchTime(prefetchTime).build();
        RefreshResult<String> copy = original.toBuilder().build();

        assertNotSame(original, copy);
        assertEquals(original.value(), copy.value());
        assertEquals(original.staleTime(), copy.staleTime());
        assertEquals(original.prefetchTime(), copy.prefetchTime());
    }
}
