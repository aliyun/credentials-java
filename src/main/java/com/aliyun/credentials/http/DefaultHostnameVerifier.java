package com.aliyun.credentials.http;

import okhttp3.internal.tls.OkHostnameVerifier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class DefaultHostnameVerifier implements HostnameVerifier {
    private boolean ignoreSSLCert = false;
    private static final HostnameVerifier NOOP_INSTANCE = new DefaultHostnameVerifier(true);

    private DefaultHostnameVerifier(boolean ignoreSSLCert) {
        this.ignoreSSLCert = ignoreSSLCert;
    }

    public static HostnameVerifier getInstance(boolean ignoreSSLCert) {
        if (ignoreSSLCert) {
            return NOOP_INSTANCE;
        } else {
            return OkHostnameVerifier.INSTANCE;
        }
    }

    @Override
    public boolean verify(String s, SSLSession sslSession) {
        return ignoreSSLCert;
    }
}
