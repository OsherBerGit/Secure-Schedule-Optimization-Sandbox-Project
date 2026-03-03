package com.example.mainbackend.algorithm.dto;

import lombok.*;
import java.util.List;

/** Top-level request body sent to POST /api/v1/algo/schedule. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoScheduleRequest {
    /** "GREEDY" or "ROUND_ROBIN" */
    private String strategy;
    private List<AlgoUserRequest> users;
    private List<AlgoTaskRequest> tasks;
}

