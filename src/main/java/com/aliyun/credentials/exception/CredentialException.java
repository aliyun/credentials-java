package com.aliyun.credentials.exception;

import java.io.Serializable;

public class CredentialException extends Exception implements Serializable {

    private static final long serialVersionUID = 634786425123290588L;

    public CredentialException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
