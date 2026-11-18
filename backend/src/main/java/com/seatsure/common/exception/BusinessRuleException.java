package com.seatsure.common.exception;

import org.springframework.http.HttpStatus;

/** A well-formed request that breaks a business rule (e.g. too many seats, booking already expired). */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
