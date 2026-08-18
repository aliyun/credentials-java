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

    /**
     * Formats HTTP failure details for CredentialException messages.
     * Includes status code, response message and body; never request secrets.
     */
    public String toHttpFailureString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HttpCode: ").append(responseCode);
        if (responseMessage != null && !responseMessage.isEmpty()) {
            sb.append(", ResponseMessage: ").append(responseMessage);
        }
        sb.append(", result: ").append(getHttpContentString());
        return sb.toString();
    }
}
