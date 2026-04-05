package com.example.mainbackend.dto.constrainttype;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConstraintTypeResponseDto {
    private Long id;
    private String name;
    private String description;
}
