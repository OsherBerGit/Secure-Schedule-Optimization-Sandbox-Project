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
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;

    private String userName;

    private String statusName;
}
