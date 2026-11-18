package com.seatsure.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/** Uniform error body returned by every endpoint. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code, message, path, Map.of());
    }

    public static ErrorResponse withFieldErrors(
            int status, String code, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, code, message, path, fieldErrors);
    }
}
