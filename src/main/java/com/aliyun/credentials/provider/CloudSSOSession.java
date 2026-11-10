package com.aliyun.credentials.provider;

import com.aliyun.credentials.exception.CredentialException;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class CloudSSOSession {
    private String name;
    @SerializedName("sign_in_url")
    String signInUrl;
    @SerializedName("access_token")
    String accessToken;
    @SerializedName("expires_at")
    private long expiresAt;

    static CloudSSOSession load(String configFile, String defaultConfigFile, String name) {
        if (!name.matches("[a-z0-9_-]{1,64}")) {
            throw new CredentialException("Invalid CloudSSO session name.");
        }
        CloudSSOSession session;
        try {
            Path config = Paths.get(configFile).toRealPath();
            Path defaults = Paths.get(defaultConfigFile);
            Path dir = config.getParent().resolve("sso");
            if (!Files.exists(defaults) || !config.equals(defaults.toRealPath())) {
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(config.toString().getBytes(StandardCharsets.UTF_8));
                StringBuilder hex = new StringBuilder();
                for (byte value : hash) {
                    hex.append(String.format("%02x", value & 0xff));
                }
                dir = dir.resolve(hex.toString());
            }
            try (Reader reader = Files.newBufferedReader(dir.resolve(name + ".json"), StandardCharsets.UTF_8)) {
                session = new Gson().fromJson(reader, CloudSSOSession.class);
            }
        } catch (IOException | RuntimeException | NoSuchAlgorithmException e) {
            throw new CredentialException("Unable to read CloudSSO session " + name);
        }
        if (session == null || !name.equals(session.name) || session.signInUrl == null || session.signInUrl.isEmpty()) {
            throw new CredentialException("Invalid CloudSSO session " + name);
        }
        if (session.accessToken == null || session.accessToken.isEmpty() || session.expiresAt <= System.currentTimeMillis() / 1000) {
            throw new CredentialException("CloudSSO session " + name + " access token is empty or expired, please re-login with CLI.");
        }
        return session;
    }
}
