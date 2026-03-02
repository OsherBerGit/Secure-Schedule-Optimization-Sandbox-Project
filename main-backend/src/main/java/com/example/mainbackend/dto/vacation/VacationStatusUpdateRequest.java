package com.example.mainbackend.dto.vacation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

/**
 * DTO for an ADMIN updating the status of a vacation request.
 * Only APPROVED or REJECTED are valid target statuses.
 */
@Data
@Builder
public class VacationStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
        regexp = "APPROVED|REJECTED",
        message = "Status must be either APPROVED or REJECTED"
    )
    private String status;
}

