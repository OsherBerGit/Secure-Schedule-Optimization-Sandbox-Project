package com.example.mainbackend.algorithm.dto;

import lombok.*;
import java.time.LocalDate;

/** Vacation block sent to the algorithm service. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoVacationRequest {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
}
