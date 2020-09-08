package com.aliyun.credentials.utils;

import com.aliyun.credentials.exception.CredentialException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class AcsURLEncoder {
    public final static String URL_ENCODING = "UTF-8";

    public static String encode(String value) {
        try {
            return URLEncoder.encode(value, URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new CredentialException(e.getMessage(), e);
        }
    }

    public static String percentEncode(String value) {
        try {
            return value != null ? URLEncoder.encode(value, URL_ENCODING).replace("+", "%20")
                    .replace("*", "%2A").replace("%7E", "~") : null;
        } catch (UnsupportedEncodingException e) {
            throw new CredentialException(e.getMessage(), e);
        }
    }
}
