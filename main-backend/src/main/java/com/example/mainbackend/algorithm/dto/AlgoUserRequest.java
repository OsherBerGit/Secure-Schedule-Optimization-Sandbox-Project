package com.example.mainbackend.algorithm.dto;

import lombok.*;
import java.util.List;
import java.util.Set;

/** Employee data sent to the algorithm service. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoUserRequest {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Integer dailyAvailabilityHours;
    private Integer maxTasks;
    private Set<String> roles;
    private List<AlgoVacationRequest> vacations;
}

