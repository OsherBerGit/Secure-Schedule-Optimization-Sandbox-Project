package com.example.mainbackend.dto.vacation;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class VacationResponseDto {
    private Long id;
    private Long workerId;
    private LocalDate startDate;
    private LocalDate endDate;

    // Display name for UI
    private String workerName;
}
