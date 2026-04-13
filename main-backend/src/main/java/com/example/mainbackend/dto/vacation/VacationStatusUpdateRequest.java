package com.example.mainbackend.dto.vacation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VacationStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
        regexp = "APPROVED|REJECTED",
        message = "Status must be either APPROVED or REJECTED"
    )
    private String status;
}
