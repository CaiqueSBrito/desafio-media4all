package com.teams_tracking_system.integrations;

public class ExternalGpsRetryStats {

    private int retryAttempts;
    private int rateLimitErrors;
    private int serviceUnavailableErrors;

    public ExternalGpsRetryStats() {
    }

    private ExternalGpsRetryStats(
            int retryAttempts,
            int rateLimitErrors,
            int serviceUnavailableErrors) {
        this.retryAttempts = retryAttempts;
        this.rateLimitErrors = rateLimitErrors;
        this.serviceUnavailableErrors = serviceUnavailableErrors;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public int getRateLimitErrors() {
        return rateLimitErrors;
    }

    public int getServiceUnavailableErrors() {
        return serviceUnavailableErrors;
    }

    public void recordRetry(int statusCode) {
        retryAttempts++;
        if (statusCode == 429) {
            rateLimitErrors++;
        }
        if (statusCode == 503) {
            serviceUnavailableErrors++;
        }
    }

    ExternalGpsRetryStats copy() {
        return new ExternalGpsRetryStats(
                retryAttempts,
                rateLimitErrors,
                serviceUnavailableErrors);
    }
}
