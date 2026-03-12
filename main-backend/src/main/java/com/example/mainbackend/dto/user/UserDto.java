package com.example.mainbackend.dto.user;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class UserDto {
    private Long id;
    private String nationalId; // Israeli National ID (Teudat Zehut)
    private String firstName;
    private String lastName;

    private String email;
    private String phoneNumber;

    private Double salary;
    private String address;
    private Integer maxTasks;

    /** Weekly availability windows (shifts) for this worker. */
    private List<WorkerAvailabilityDto> availabilities;

    /** Name of the department this user belongs to (null if unassigned). */
    private String departmentName;

    // Roles as role names (not full entity to avoid circular references)
    private Set<String> roles;
}
