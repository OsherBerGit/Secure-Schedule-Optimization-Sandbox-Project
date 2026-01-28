package com.example.mainbackend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class UserDto {
    private Long id;
    private String teudatZehut;
    private String firstName;
    private String lastName;

    private String email;
    private String phoneNumber;

    private Double salary;
    private String address;
    private Integer dailyAvailabilityHours;
    private Integer maxTasks;

    // Roles as role names (not full entity to avoid circular references)
    private Set<String> roles;

    // Vacation and Settlement IDs (not full objects to keep DTO lightweight)
    private List<Long> vacationIds;
    private List<Long> settlementIds;
}
