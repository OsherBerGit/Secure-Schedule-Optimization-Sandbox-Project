package com.example.mainbackend.dto.status;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatusResponseDto {
    private Long id;
    private String name;
}

