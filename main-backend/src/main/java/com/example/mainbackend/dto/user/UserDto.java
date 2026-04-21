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

    private Integer maxTasks;

    private List<UserAvailabilityDto> availabilities;

    private String departmentName;

    private String role;

    private Set<SkillDto> skills;
    private Set<Long> skillIds;
}
