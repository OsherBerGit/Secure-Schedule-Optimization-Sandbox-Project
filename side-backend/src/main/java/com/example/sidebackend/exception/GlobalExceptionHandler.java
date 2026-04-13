package com.example.sidebackend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── 400: Bean-validation failures (@Valid) ───────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        log.warn("[GlobalExceptionHandler] Validation failed: {}", errors);
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    // ─── 400: Sanitization failures (AlgoService) ────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        log.warn("[GlobalExceptionHandler] Sanitization error: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    // ─── 500: Unexpected errors ───────────────────────────────────────────────

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, Object>> handleAll(Throwable ex) {
        log.error("[GlobalExceptionHandler] Critical/Unexpected error", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "Critical error: " + ex.getMessage(), List.of(ex.toString()));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status, String message, List<String> details) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (!details.isEmpty()) body.put("details", details);
        return ResponseEntity.status(status).body(body);
    }
}
