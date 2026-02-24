package com.example.mainbackend.dto.constrainttype;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConstraintTypeResponseDto {
    private Long id;
    private String name;
    private String description;
}

