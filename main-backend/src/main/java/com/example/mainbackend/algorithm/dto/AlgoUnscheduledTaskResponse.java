package com.example.mainbackend.algorithm.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoUnscheduledTaskResponse {
    private Long   taskId;
    private String taskName;
    private String reason;
}

