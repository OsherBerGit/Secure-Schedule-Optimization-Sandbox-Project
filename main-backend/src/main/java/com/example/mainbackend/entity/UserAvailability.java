package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Represents a specific weekly availability window (shift) for a user.
 *
 * <p>Replaces the coarse-grained {@code dailyAvailabilityHours} integer with
 * precise per-day time ranges so the scheduler can make accurate assignments.</p>
 *
 * <p>Examples:
 * <ul>
 *   <li>MONDAY   09:00 – 17:00  (standard day shift)</li>
 *   <li>SATURDAY 08:00 – 12:00  (half-day)</li>
 * </ul>
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_availabilities")
public class UserAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /** The day of the week this availability window applies to. */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    /** Start of the available window (inclusive). */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /** End of the available window (exclusive). */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** The worker this availability window belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
