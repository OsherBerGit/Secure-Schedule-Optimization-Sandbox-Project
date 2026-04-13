package com.example.sidebackend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record VacationDto(

        Long id,

        @NotNull(message = "Vacation startDate must not be null")
        LocalDate startDate,

        @NotNull(message = "Vacation endDate must not be null")
        LocalDate endDate

) {}
