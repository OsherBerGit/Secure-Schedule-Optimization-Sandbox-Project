package com.example.mainbackend.algorithm.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoConstraintRequest {
    private Long predecessorId;

    // Type of constraint: FS (Default), SS, FF, SF.
    private ConstraintType type;
}
