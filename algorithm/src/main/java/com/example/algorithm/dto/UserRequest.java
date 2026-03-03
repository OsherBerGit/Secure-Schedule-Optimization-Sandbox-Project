package com.example.algorithm.dto;

import lombok.*;

import java.util.List;
import java.util.Set;

/**
 * Represents an employee sent from main-backend for scheduling.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Integer dailyAvailabilityHours;
    private Integer maxTasks;

    /** Roles this employee holds (e.g. "WORKER", "ADMIN") */
    private Set<String> roles;

    /** Only APPROVED vacations should be included */
    private List<VacationRequest> vacations;
}

