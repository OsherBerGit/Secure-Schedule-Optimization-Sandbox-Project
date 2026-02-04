package com.example.mainbackend.dto.vacation;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class VacationCreateRequest {
    private Long workerId;
    private LocalDate startDate;
    private LocalDate endDate;
}
