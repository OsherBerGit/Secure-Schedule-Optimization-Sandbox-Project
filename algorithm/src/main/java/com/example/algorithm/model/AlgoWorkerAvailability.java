package com.example.algorithm.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Represents one weekly availability window (shift) for a worker.
 *
 * <p>Pure Java record — no Lombok, no Spring, no JPA.
 * Immutable by definition (Java record semantics).</p>
 *
 * <p>Examples:
 * <ul>
 *   <li>MONDAY   09:00 – 17:00  (standard day shift)</li>
 *   <li>SATURDAY 08:00 – 12:00  (half-day)</li>
 * </ul>
 * </p>
 *
 * @param dayOfWeek the day of the week this window applies to (never null)
 * @param startTime start of the available window, inclusive (never null)
 * @param endTime   end of the available window, exclusive (never null)
 */
public record AlgoWorkerAvailability(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {}

