package com.aliyun.credentials.provider;

public final class RefreshResult<T> {
    private final T value;
    private final long staleTime;
    private final long prefetchTime;

    private RefreshResult(Builder<T> builder) {
        this.value = builder.value;
        this.staleTime = builder.staleTime;
        this.prefetchTime = builder.prefetchTime;
    }

    public static <T> Builder<T> builder(T value) {
        return new Builder<>(value);
    }

    public T value() {
        return value;
    }

    public long staleTime() {
        return staleTime;
    }

    public long prefetchTime() {
        return prefetchTime;
    }

    @Override
    public String toString() {
        return String.format("RefreshResult(value=%s, staleTime=%d, prefetchTime=%d)", value, staleTime, prefetchTime);
    }

    public RefreshResult.Builder<T> toBuilder() {
        return new RefreshResult.Builder<>(this);
    }

    public static final class Builder<T> {
        private final T value;
        private long staleTime = Long.MAX_VALUE;
        private long prefetchTime = Long.MAX_VALUE;

        private Builder(T value) {
            this.value = value;
        }

        private Builder(RefreshResult<T> value) {
            this.value = value.value;
            this.staleTime = value.staleTime;
            this.prefetchTime = value.prefetchTime;
        }

        public Builder<T> staleTime(long staleTime) {
            this.staleTime = staleTime;
            return this;
        }

        public Builder<T> prefetchTime(long prefetchTime) {
            this.prefetchTime = prefetchTime;
            return this;
        }

        public RefreshResult<T> build() {
            return new RefreshResult<T>(this);
        }
    }
}
