package com.example.mainbackend.exception;

import lombok.Getter;

import java.util.List;

/**
 * Thrown when a batch operation (like scheduling) fails multiple validation checks.
 * Holds the list of all errors to return to the client.
 */
@Getter
public class BatchValidationException extends RuntimeException {
    
    private final List<String> errors;

    public BatchValidationException(List<String> errors) {
        super("Batch validation failed with " + errors.size() + " errors.");
        this.errors = errors;
    }
}

