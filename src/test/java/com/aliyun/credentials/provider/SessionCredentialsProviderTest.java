package com.aliyun.credentials.provider;

import com.aliyun.credentials.models.CredentialModel;
import org.junit.Assert;
import org.junit.Test;

public class SessionCredentialsProviderTest {

    @Test
    public void builderHooksAndStaleTime() {
        TestBuilder builder = new TestBuilder()
                .asyncCredentialUpdateEnabled(true)
                .staleValueBehavior(RefreshCachedSupplier.StaleValueBehavior.ALLOW)
                .jitterEnabled(false);
        RecordingProvider provider = builder.build();
        Assert.assertTrue(provider.isAsyncCredentialUpdateEnabled());

        long now = System.currentTimeMillis();
        Assert.assertTrue(provider.getStaleTime(0) >= now);
        long future = now + 60 * 60 * 1000;
        Assert.assertEquals(future - RefreshCachedSupplier.STALE_TIME, provider.getStaleTime(future));
        provider.close();
    }

    private static final class TestBuilder
            extends SessionCredentialsProvider.BuilderImpl<RecordingProvider, TestBuilder> {
        @Override
        public RecordingProvider build() {
            return new RecordingProvider(this);
        }
    }

    private static final class RecordingProvider extends SessionCredentialsProvider {
        private RecordingProvider(TestBuilder builder) {
            super(builder);
        }

        @Override
        public RefreshResult<CredentialModel> refreshCredentials() {
            return RefreshResult.<CredentialModel>builder(CredentialModel.builder()
                            .accessKeyId("ak")
                            .accessKeySecret("sk")
                            .build())
                    .staleTime(System.currentTimeMillis() + 3600_000L)
                    .build();
        }

        @Override
        public String getProviderName() {
            return "test";
        }
    }
}
