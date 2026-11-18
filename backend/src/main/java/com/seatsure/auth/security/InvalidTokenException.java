package com.seatsure.auth.security;

import com.seatsure.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidTokenException extends ApiException {

    public InvalidTokenException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Token is invalid or expired");
    }
}
