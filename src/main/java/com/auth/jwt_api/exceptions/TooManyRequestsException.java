package com.auth.jwt_api.exceptions;

import java.util.concurrent.TimeUnit;

public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(long nanosToWaitForRefill) {
        super("Too many requests. Please try again later.");
        this.retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(nanosToWaitForRefill) + 1;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
