package com.example.algorithm.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

// Represents one weekly availability window (shift) for a worker.
// MONDAY 09:00 – 17:00 (standard day shift)
// SATURDAY 08:00 – 12:00 (half-day)
public record AlgoWorkerAvailability(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {}

