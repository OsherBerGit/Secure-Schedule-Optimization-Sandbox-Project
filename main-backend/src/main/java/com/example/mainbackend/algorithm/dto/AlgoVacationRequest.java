package com.example.mainbackend.algorithm.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoVacationRequest {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
}
