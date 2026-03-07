package com.example.sidebackend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Anonymous approved vacation window for a worker.
 *
 * <p>Only APPROVED vacations are sent by the main-backend.
 * Status is intentionally excluded — the algorithm treats every
 * vacation in this list as a blocked availability window.</p>
 *
 * @param id         Internal database ID of the vacation record
 * @param startDate  First day of the vacation (inclusive)
 * @param endDate    Last day of the vacation (inclusive)
 */
public record VacationDto(

        Long id,

        @NotNull(message = "Vacation startDate must not be null")
        LocalDate startDate,

        @NotNull(message = "Vacation endDate must not be null")
        LocalDate endDate

) {}

