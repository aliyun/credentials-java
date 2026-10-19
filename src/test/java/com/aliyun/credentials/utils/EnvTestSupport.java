package com.aliyun.credentials.utils;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Test-only helper that updates the process environment. Java 16+ may deny
 * reflective access; callers should skip the test when {@link #trySet} is false.
 */
public final class EnvTestSupport {
    private EnvTestSupport() {
    }

    @SuppressWarnings("unchecked")
    public static boolean trySet(String key, String value) {
        try {
            Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
            try {
                updateMap(processEnvironment.getDeclaredField("theEnvironment"), key, value);
                updateMap(processEnvironment.getDeclaredField("theCaseInsensitiveEnvironment"), key, value);
            } catch (NoSuchFieldException unixFieldsMissing) {
                Map<String, String> env = System.getenv();
                Field field = env.getClass().getDeclaredField("m");
                field.setAccessible(true);
                Map<String, String> writable = (Map<String, String>) field.get(env);
                putOrRemove(writable, key, value);
            }
            return System.getenv(key) == null ? value == null : value != null && value.equals(System.getenv(key));
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void updateMap(Field field, String key, String value) throws Exception {
        field.setAccessible(true);
        Map<String, String> writable = (Map<String, String>) field.get(null);
        putOrRemove(writable, key, value);
    }

    private static void putOrRemove(Map<String, String> writable, String key, String value) {
        if (value == null) {
            writable.remove(key);
        } else {
            writable.put(key, value);
        }
    }
}
