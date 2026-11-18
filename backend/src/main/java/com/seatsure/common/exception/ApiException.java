package com.seatsure.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for every error we raise on purpose. Carries the HTTP status and a stable
 * machine-readable code so the frontend never has to parse messages.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
