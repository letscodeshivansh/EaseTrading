package com.easetrading.api.common;

import org.springframework.http.HttpStatus;

/**
 * A simple application exception that carries an HTTP status.
 * Throw this anywhere in the code to return a clean error to the client,
 * e.g. throw new ApiException(HttpStatus.NOT_FOUND, "Instrument not found").
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
