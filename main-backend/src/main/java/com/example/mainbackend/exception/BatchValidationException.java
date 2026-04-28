package com.example.mainbackend.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class BatchValidationException extends RuntimeException {
    
    private final List<String> errors;

    public BatchValidationException(List<String> errors) {
        super("Batch validation failed with " + errors.size() + " errors.");
        this.errors = errors;
    }
}

