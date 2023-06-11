package com.aliyun.credentials.provider;

public final class RefreshResult<T> {
    private final T value;
    private final long staleTime;

    private RefreshResult(Builder<T> builder) {
        this.value = builder.value;
        this.staleTime = builder.staleTime;
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

    public static final class Builder<T> {
        private final T value;
        private long staleTime;

        private Builder(T value) {
            this.value = value;
        }

        public Builder<T> staleTime(long staleTime) {
            this.staleTime = staleTime;
            return this;
        }

        public RefreshResult<T> build() {
            return new RefreshResult<T>(this);
        }
    }
}
