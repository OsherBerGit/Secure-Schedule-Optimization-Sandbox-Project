package com.example.mainbackend.dto.vacation;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for a WORKER submitting their own vacation request.
 * The worker's identity is extracted from the JWT Security Context,
 * so no workerId is needed in the request body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VacationRequestDto {

    @NotNull(message = "Worker ID is required")
    private Long workerId;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;
}

