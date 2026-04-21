package com.example.mainbackend.algorithm.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoTaskRequest {
    private Long id;
    private Integer durationHours;
    private LocalDateTime deadline;
    private Integer priorityLevel;

    private Set<Long> requiredSkillIds;

    private List<AlgoConstraintRequest> constraints;
}
