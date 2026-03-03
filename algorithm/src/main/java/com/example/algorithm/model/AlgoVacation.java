package com.example.algorithm.model;

import lombok.*;

import java.time.LocalDate;

/**
 * Represents an approved vacation period for an employee.
 * Used by the algorithm to block out unavailable days.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AlgoVacation {

    private Long id;
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}

