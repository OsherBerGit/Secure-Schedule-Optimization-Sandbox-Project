package com.example.mainbackend.dto.vacation;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationResponseDto {
    private Long id;
    private Long workerId;
    private LocalDate startDate;
    private LocalDate endDate;

    // Display name for UI
    private String workerName;

    // Vacation request status (PENDING, APPROVED, REJECTED)
    private String statusName;
}
