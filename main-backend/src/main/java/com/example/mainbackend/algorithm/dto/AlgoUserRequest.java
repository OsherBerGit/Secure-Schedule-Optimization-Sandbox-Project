package com.example.mainbackend.algorithm.dto;

import com.example.mainbackend.dto.user.UserAvailabilityDto;
import lombok.*;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgoUserRequest {
    private Long id;

    private List<UserAvailabilityDto> availabilities;
    private Integer maxTasks;

    private Set<Long> skillIds;
    private List<AlgoVacationRequest> vacations;
}
