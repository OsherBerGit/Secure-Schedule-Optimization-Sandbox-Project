package com.example.mainbackend.constants;

import lombok.Getter;

@Getter
public enum ConstraintTypeLevel {
    //Finish-to-Start: Successor cannot start until predecessor finishes. Most common constraint type.
    FINISH_TO_START("Successor cannot start until predecessor finishes"),

    // Start-to-Start: Successor cannot start until predecessor starts.
    START_TO_START("Successor cannot start until predecessor starts"),

    // Finish-to-Finish: Successor cannot finish until predecessor finishes.
    FINISH_TO_FINISH("Successor cannot finish until predecessor finishes"),

    // Start-to-Finish: Successor cannot finish until predecessor starts. Rarely used.
    START_TO_FINISH("Successor cannot finish until predecessor starts");

    private final String description;

    ConstraintTypeLevel(String description) { this.description = description; }
}
