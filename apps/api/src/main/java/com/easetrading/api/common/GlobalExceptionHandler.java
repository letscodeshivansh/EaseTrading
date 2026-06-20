package com.easetrading.api.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Turns exceptions into consistent JSON error responses so the frontend always
 * receives the same shape: { timestamp, status, message }.
 * Centralising this here means controllers stay clean and never build error bodies.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Our own typed exceptions carry the intended HTTP status. */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        return build(ex.getStatus(), ex.getMessage());
    }

    /** Any unexpected error becomes a safe 500 (no stack traces leak to clients). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: " + ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", status.value(),
            "message", message
        ));
    }
}
