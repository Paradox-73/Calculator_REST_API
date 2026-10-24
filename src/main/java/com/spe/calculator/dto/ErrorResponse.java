package com.spe.calculator.dto;

import java.time.Instant;

/**
 * Structured error body returned for every failed request.
 */
public record ErrorResponse(String timestamp, int status, String error, String message, String path) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now().toString(), status, error, message, path);
    }
}
