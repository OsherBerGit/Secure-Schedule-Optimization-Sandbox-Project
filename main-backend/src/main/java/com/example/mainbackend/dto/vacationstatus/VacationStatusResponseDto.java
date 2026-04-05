package com.example.mainbackend.dto.vacationstatus;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationStatusResponseDto {
    private Long id;
    private String name;
}