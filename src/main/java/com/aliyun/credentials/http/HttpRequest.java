package com.aliyun.credentials.http;

import com.aliyun.credentials.utils.ParameterHelper;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest extends HttpMessage {
    private Map<String, String> immutableMap = new HashMap<String, String>();

    public HttpRequest() {
        setCommonParameter();
    }

    public HttpRequest(String url) {
        super(url);
        setCommonParameter();
    }

    private void setCommonParameter() {
        this.immutableMap.put("Timestamp", ParameterHelper.getISO8601Time(new Date()));
        this.immutableMap.put("SignatureNonce", ParameterHelper.getUniqueNonce());
        this.immutableMap.put("SignatureMethod", "HMAC-SHA1");
        this.immutableMap.put("SignatureVersion", "1.0");
    }

    public void setUrlParameter(String key, String value) {
        this.immutableMap.put(key, value);
    }

    public String getUrlParameter(String key) {
        return this.immutableMap.get(key);
    }

    public Map<String, String> getUrlParameters() {
        return Collections.unmodifiableMap(immutableMap);
    }

}
