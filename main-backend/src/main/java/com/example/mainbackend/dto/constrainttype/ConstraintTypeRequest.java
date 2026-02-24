package com.example.mainbackend.dto.constrainttype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConstraintTypeRequest {

    @NotBlank(message = "Constraint type name is required")
    @Size(min = 2, max = 50, message = "Constraint type name must be between 2 and 50 characters")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
}

