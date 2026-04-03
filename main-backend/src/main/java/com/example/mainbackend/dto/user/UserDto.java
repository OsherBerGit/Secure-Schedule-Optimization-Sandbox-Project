package com.example.mainbackend.dto.user;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.example.mainbackend.dto.skill.SkillDto;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String nationalId;
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

    // Access Level: Single role (ADMIN, MANAGER, WORKER)
    private String role;

    // Functional Skills
    private Set<SkillDto> skills;
    private Set<Long> skillIds;
}
