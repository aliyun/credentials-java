package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.models.CredentialModel;
import com.aliyun.credentials.utils.AuthConstant;
import com.aliyun.credentials.utils.CommandLineUtils;
import com.aliyun.credentials.utils.ProviderName;
import com.aliyun.credentials.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.concurrent.TimeUnit;

public class ExternalCredentialsProvider implements AlibabaCloudCredentialsProvider {

    @FunctionalInterface
    public interface ExternalCredentialUpdateCallback {
        void onCredentialUpdate(String accessKeyId, String accessKeySecret,
                                String securityToken, long expiration) throws Exception;
    }

    private static final long EXPIRATION_SLOT_SECONDS = 180;
    private final String processCommand;
    private final int timeoutMilliseconds;
    private final ExternalCredentialUpdateCallback credentialUpdateCallback;
    private final Object lock = new Object();
    private volatile CredentialModel credential;
    private volatile long expirationTimestamp;

    private ExternalCredentialsProvider(Builder builder) {
        if (StringUtils.isEmpty(builder.processCommand)) {
            throw new IllegalArgumentException("process_command is empty");
        }
        this.processCommand = builder.processCommand;
        Integer timeout = builder.timeoutMilliseconds;
        this.timeoutMilliseconds = timeout == null ? 60 * 1000 : timeout;
        this.credentialUpdateCallback = builder.credentialUpdateCallback;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CredentialModel getCredentials() {
        if (needUpdateCredential()) {
            synchronized (lock) {
                if (needUpdateCredential()) {
                    CredentialModel refreshed = getCredentialsInternal();
                    this.credential = refreshed;
                    this.expirationTimestamp = refreshed.getExpiration() > 0 ? refreshed.getExpiration() / 1000 : 0;
                    invokeCredentialUpdateCallback(refreshed);
                }
            }
        }
        return CredentialModel.builder()
                .accessKeyId(this.credential.getAccessKeyId())
                .accessKeySecret(this.credential.getAccessKeySecret())
                .securityToken(this.credential.getSecurityToken())
                .type(this.credential.getType())
                .providerName(this.getProviderName())
                .expiration(this.credential.getExpiration())
                .build();
    }

    CredentialModel getCredentialsInternal() {
        String[] args = CommandLineUtils.split(this.processCommand);

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(this.timeoutMilliseconds, TimeUnit.MILLISECONDS);
            String stdout = readAll(process.getInputStream());
            String stderr = readAll(process.getErrorStream());
            if (!finished) {
                process.destroyForcibly();
                throw new CredentialException(
                        String.format("command process timed out after %d milliseconds", this.timeoutMilliseconds));
            }
            if (process.exitValue() != 0) {
                throw new CredentialException(String.format(
                        "failed to execute external command: exit status %d\nstderr: %s",
                        process.exitValue(), stderr));
            }
            return parseCredentialResponse(stdout);
        } catch (CredentialException e) {
            throw e;
        } catch (Exception e) {
            throw new CredentialException("failed to execute external command: " + e.getMessage(), e);
        }
    }

    private CredentialModel parseCredentialResponse(String stdout) {
        ExternalCredentialResponse response;
        try {
            response = new Gson().fromJson(stdout, ExternalCredentialResponse.class);
        } catch (JsonSyntaxException e) {
            throw new CredentialException("failed to parse external command output: " + e.getMessage(), e);
        }
        if (response == null) {
            throw new CredentialException("failed to parse external command output: empty response");
        }
        if (StringUtils.isEmpty(response.accessKeyId) || StringUtils.isEmpty(response.accessKeySecret)) {
            throw new CredentialException("invalid credential response: access_key_id or access_key_secret is empty");
        }
        if ("StsToken".equals(response.mode) && StringUtils.isEmpty(response.securityToken)) {
            throw new CredentialException("invalid StsToken credential response: sts_token is empty");
        }

        long expiration = parseExpiration(response.expiration);
        return CredentialModel.builder()
                .accessKeyId(response.accessKeyId)
                .accessKeySecret(response.accessKeySecret)
                .securityToken(response.securityToken)
                .type(StringUtils.isEmpty(response.securityToken) ? AuthConstant.ACCESS_KEY : AuthConstant.STS)
                .providerName(this.getProviderName())
                .expiration(expiration)
                .build();
    }

    private long parseExpiration(String expiration) {
        if (StringUtils.isEmpty(expiration)) {
            return 0;
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(new SimpleTimeZone(0, "UTC"));
        try {
            Date date = df.parse(expiration);
            return date.getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    private boolean needUpdateCredential() {
        if (this.credential == null) {
            return true;
        }
        if (this.expirationTimestamp == 0) {
            return true;
        }
        return this.expirationTimestamp - System.currentTimeMillis() / 1000 <= EXPIRATION_SLOT_SECONDS;
    }

    private void invokeCredentialUpdateCallback(CredentialModel refreshed) {
        if (this.credentialUpdateCallback == null) {
            return;
        }
        try {
            this.credentialUpdateCallback.onCredentialUpdate(
                    refreshed.getAccessKeyId(),
                    refreshed.getAccessKeySecret(),
                    refreshed.getSecurityToken(),
                    this.expirationTimestamp);
        } catch (Exception e) {
            // Warning only, do not break credential retrieval
        }
    }

    private String readAll(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, count);
        }
        return outputStream.toString("UTF-8");
    }

    @Override
    public String getProviderName() {
        return ProviderName.EXTERNAL;
    }

    @Override
    public void close() {
    }

    public static final class Builder {
        private String processCommand;
        private Integer timeoutMilliseconds;
        private ExternalCredentialUpdateCallback credentialUpdateCallback;

        public Builder processCommand(String processCommand) {
            this.processCommand = processCommand;
            return this;
        }

        public Builder timeoutMilliseconds(Integer timeoutMilliseconds) {
            this.timeoutMilliseconds = timeoutMilliseconds;
            return this;
        }

        public Builder credentialUpdateCallback(ExternalCredentialUpdateCallback callback) {
            this.credentialUpdateCallback = callback;
            return this;
        }

        public ExternalCredentialsProvider build() {
            return new ExternalCredentialsProvider(this);
        }
    }

    private static class ExternalCredentialResponse {
        @SerializedName("mode")
        private String mode;
        @SerializedName("access_key_id")
        private String accessKeyId;
        @SerializedName("access_key_secret")
        private String accessKeySecret;
        @SerializedName("sts_token")
        private String securityToken;
        @SerializedName("expiration")
        private String expiration;
    }
}
