package com.aliyun.credentials.exception;

public class CredentialException extends RuntimeException {

    private static final long serialVersionUID = 634786425123290588L;

    public CredentialException(String message) {
        super(message);
    }

    public CredentialException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
