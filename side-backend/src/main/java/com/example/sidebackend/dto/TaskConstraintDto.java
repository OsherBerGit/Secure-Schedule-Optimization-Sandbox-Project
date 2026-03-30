package com.example.sidebackend.dto;

public record TaskConstraintDto(
        Long predecessorId,
        ConstraintType type
) {}
