package com.aliyun.credentials.http;


import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.utils.Base64Helper;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class HttpMessage {

    protected static final String CONTENT_TYPE = "Content-Type";
    protected static final String CONTENT_MD5 = "Content-MD5";
    protected static final String CONTENT_LENGTH = "Content-Length";
    protected FormatType httpContentType = null;
    protected byte[] httpContent = null;
    protected String encoding = null;
    protected Map<String, String> headers = new HashMap<String, String>();
    protected Integer connectTimeout = null;
    protected Integer readTimeout = null;
    private String url = null;
    private MethodType method = null;

    public HttpMessage(String strUrl) {
        this.url = strUrl;
    }

    public HttpMessage() {
    }

    public FormatType getHttpContentType() {
        return httpContentType;
    }

    public void putHeaderParameter(String name, String value) {
        if (null != name && null != value) {
            this.headers.put(name, value);
        }
    }

    public void setHttpContentType(FormatType httpContentType) {
        this.httpContentType = httpContentType;
    }

    public byte[] getHttpContent() {
        return httpContent;
    }

    public void setHttpContent(byte[] content, String encoding, FormatType format) {
        if (null == content) {
            this.headers.remove(CONTENT_MD5);
            this.headers.put(CONTENT_LENGTH, "0");
            this.headers.remove(CONTENT_TYPE);
            this.httpContentType = null;
            this.httpContent = null;
            this.encoding = null;
            return;
        }

        // for GET HEADER DELETE OPTION method, sdk should ignore the content
        if (getSysMethod() != null && !getSysMethod().hasContent()) {
            content = new byte[0];
        }

        this.httpContent = content;
        this.encoding = encoding;
        String contentLen = String.valueOf(content.length);
        String strMd5 = md5Sum(content);
        this.headers.put(CONTENT_MD5, strMd5);
        this.headers.put(CONTENT_LENGTH, contentLen);
        if (null != format) {
            this.headers.put(CONTENT_TYPE, FormatType.mapFormatToAccept(format));
        }
    }

    public static String md5Sum(byte[] buff) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(buff);
            return Base64Helper.encode(messageDigest);
        } catch (Exception e) {
            throw new CredentialException(e.getMessage(), e);
        }
    }


    public String getHeaderValue(String name) {
        return this.headers.get(name);
    }


    public String getHttpContentString() {
        String stringContent = "";
        if (this.httpContent != null) {
            try {
                if (this.encoding == null) {
                    stringContent = new String(this.httpContent);
                } else {
                    stringContent = new String(this.httpContent, this.encoding);
                }
            } catch (UnsupportedEncodingException exp) {
                throw new CredentialException("Can not parse response due to unsupported encoding: " + exp.getMessage(), exp);
            }
        }
        return stringContent;
    }


    public String getSysUrl() {
        return url;
    }

    public void setSysUrl(String url) {
        this.url = url;
    }

    public MethodType getSysMethod() {
        return method;
    }

    public void setSysMethod(MethodType method) {
        this.method = method;
        //This is the pop rule and the put method accepts only json data
        if (MethodType.PUT == method) {
            setHttpContentType(FormatType.JSON);
        }
    }

    public String getSysEncoding() {
        return encoding;
    }

    public void setSysEncoding(String encoding) {
        this.encoding = encoding;
    }

    public Integer getSysConnectTimeout() {
        return connectTimeout;
    }

    public void setSysConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getSysReadTimeout() {
        return readTimeout;
    }

    public void setSysReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Map<String, String> getSysHeaders() {
        return Collections.unmodifiableMap(headers);
    }
}
