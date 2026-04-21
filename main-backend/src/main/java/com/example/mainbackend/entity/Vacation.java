package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Vacation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * Vacation request status (PENDING, APPROVED, REJECTED).
     * Uses a dedicated VacationStatus lookup table, separate from TaskStatus.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vacation_status_id", nullable = false)
    private VacationStatus status;
}
