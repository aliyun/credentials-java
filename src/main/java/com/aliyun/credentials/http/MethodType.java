package com.aliyun.credentials.http;

public enum MethodType {
    GET(false),
    PUT(true),
    POST(true);

    private boolean hasContent;

    MethodType(boolean hasContent) {
        this.hasContent = hasContent;
    }

    public boolean hasContent() {
        return hasContent;
    }
}