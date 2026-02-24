package com.example.mainbackend.dto.priority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PriorityRequest {

    @NotBlank(message = "Priority name is required")
    @Size(min = 2, max = 50, message = "Priority name must be between 2 and 50 characters")
    private String name;
}

