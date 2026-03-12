package com.example.mainbackend.algorithm.dto;

import com.example.mainbackend.dto.user.WorkerAvailabilityDto;
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
    /** Specific weekly shift windows, replacing the old coarse dailyAvailabilityHours. */
    private List<WorkerAvailabilityDto> availabilities;
    private Integer maxTasks;
    private Set<Long> roles;
    private List<AlgoVacationRequest> vacations;
}

