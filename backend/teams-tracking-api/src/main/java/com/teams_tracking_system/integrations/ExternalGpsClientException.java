package com.teams_tracking_system.integrations;

public class ExternalGpsClientException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;
    private final String retryAfter;

    public ExternalGpsClientException(String message, int statusCode, String responseBody) {
        this(message, statusCode, responseBody, null);
    }

    public ExternalGpsClientException(String message, int statusCode, String responseBody, String retryAfter) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.retryAfter = retryAfter;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getRetryAfter() {
        return retryAfter;
    }
}
