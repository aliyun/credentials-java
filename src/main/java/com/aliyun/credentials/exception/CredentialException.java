package com.aliyun.credentials.exception;

import java.io.Serializable;

public class CredentialException extends RuntimeException implements Serializable {

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
