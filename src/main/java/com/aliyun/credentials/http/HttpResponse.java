package com.aliyun.credentials.http;

public class HttpResponse extends HttpMessage {
    private int responseCode;
    private String responseMessage;

    public HttpResponse(String strUrl) {
        super(strUrl);
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }
}
