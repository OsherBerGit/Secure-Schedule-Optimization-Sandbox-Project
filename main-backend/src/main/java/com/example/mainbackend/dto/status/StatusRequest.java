package com.example.mainbackend.dto.status;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatusRequest {

    @NotBlank(message = "Status name is required")
    @Size(min = 2, max = 50, message = "Status name must be between 2 and 50 characters")
    private String name;
}

