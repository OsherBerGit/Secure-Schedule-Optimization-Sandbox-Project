package com.example.mainbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Represents one weekly availability window (shift) for a worker.
 * Used in API responses and requests to/from the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerAvailabilityDto {
    private Long      id;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}

