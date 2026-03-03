package com.example.algorithm.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * Represents an approved vacation period for an employee inside a schedule request.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacationRequest {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
}

