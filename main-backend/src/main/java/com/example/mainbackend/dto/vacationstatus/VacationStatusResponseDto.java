package com.example.mainbackend.dto.vacationstatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VacationStatusResponseDto {
    private Long id;
    private String name;
}