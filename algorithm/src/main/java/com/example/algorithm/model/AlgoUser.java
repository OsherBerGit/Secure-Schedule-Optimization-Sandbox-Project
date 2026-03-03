package com.example.algorithm.model;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Represents a User/Employee as seen by the scheduling algorithm.
 * Populated from the database via DatabaseReader.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AlgoUser {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;

    /** How many hours per day this employee is available */
    private Integer dailyAvailabilityHours;

    /** Max number of tasks this employee can handle at once */
    private Integer maxTasks;

    /** Roles this employee holds (e.g. "DEVELOPER", "MANAGER") */
    private Set<String> roles;

    /** Approved vacation periods for this employee */
    private List<AlgoVacation> vacations;
}

