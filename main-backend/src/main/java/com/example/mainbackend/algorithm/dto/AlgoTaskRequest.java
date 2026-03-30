package com.example.mainbackend.algorithm.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/** Task data sent to the algorithm service. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoTaskRequest {
    private Long id;
    private Integer durationHours;
    private LocalDateTime deadline;
    private Integer priorityLevel;
    
    /** Functional skill required (Job ID). Replaces requiredRoles. */
    private Long requiredJobId;
    
    private List<AlgoConstraintRequest> constraints;
}
