package com.example.algorithm.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Represents a Task as seen by the scheduling algorithm.
 * Populated from the database via DatabaseReader.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AlgoTask {

    private Long id;
    private String title;
    private String description;
    private Integer durationHours;
    private LocalDateTime deadline;
    private LocalDateTime startTime;

    /** Priority name (e.g. "HIGH", "MEDIUM", "LOW") */
    private String priority;
    private Integer priorityLevel;

    /** Status name (e.g. "PENDING", "IN_PROGRESS", "DONE") */
    private String status;

    /** The employee assigned to this task (null if unassigned) */
    private AlgoUser assignedEmployee;

    /** Roles required to perform this task */
    private Set<String> requiredRoles;

    /** IDs of tasks that must finish before this task can start */
    private List<Long> predecessorTaskIds;

    /** IDs of tasks that depend on this task */
    private List<Long> successorTaskIds;
}

