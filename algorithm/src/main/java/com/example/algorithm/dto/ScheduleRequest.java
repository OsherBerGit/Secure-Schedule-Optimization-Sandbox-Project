package com.example.algorithm.dto;

import lombok.*;

import java.util.List;

/**
 * The top-level request body sent from main-backend to POST /api/v1/algo/schedule.
 * Contains the full snapshot of users and tasks for the algorithm to process.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRequest {

    /**
     * Scheduling strategy to use.
     * Accepted values: "GREEDY" (default), "ROUND_ROBIN"
     */
    private String strategy;

    /** All employees available for assignment */
    private List<UserRequest> users;

    /** All tasks to be scheduled (PENDING / IN_PROGRESS) */
    private List<TaskRequest> tasks;
}

