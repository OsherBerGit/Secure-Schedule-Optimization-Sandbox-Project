package com.example.mainbackend.mapper;

import com.example.mainbackend.algorithm.dto.AlgoUserRequest;
import com.example.mainbackend.algorithm.dto.AlgoVacationRequest;
import com.example.mainbackend.constants.VacationStatusLevel;
import com.example.mainbackend.dto.user.CreateUserRequest;
import com.example.mainbackend.dto.user.UserDto;
import com.example.mainbackend.dto.user.WorkerAvailabilityDto;
import com.example.mainbackend.dto.skill.SkillDto;
import com.example.mainbackend.entity.Skill;
import com.example.mainbackend.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) return null;

        return UserDto.builder()
                .id(user.getId())
                .nationalId(user.getNationalId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .salary(user.getSalary())
                .address(user.getAddress())
                .maxTasks(user.getMaxTasks())
                .availabilities(user.getAvailabilities() != null
                        ? user.getAvailabilities().stream()
                                .map(a -> WorkerAvailabilityDto.builder()
                                        .id(a.getId())
                                        .dayOfWeek(a.getDayOfWeek())
                                        .startTime(a.getStartTime())
                                        .endTime(a.getEndTime())
                                        .build())
                                .collect(Collectors.toList())
                        : Collections.emptyList())
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .role(user.getRole() != null ? user.getRole().getRoleName() : null)
                .skills(user.getSkills() != null
                        ? user.getSkills().stream()
                                .map(s -> SkillDto.builder()
                                        .id(s.getId())
                                        .name(s.getName())
                                        .description(s.getDescription())
                                        .build())
                                .collect(Collectors.toSet())
                        : Collections.emptySet())
                .skillIds(user.getSkills() != null
                        ? user.getSkills().stream().map(Skill::getId).collect(Collectors.toSet())
                        : Collections.emptySet())
                .build();
    }

    public User fromCreateRequest(CreateUserRequest request, String encodedPassword) {
        if (request == null) return null;

        return User.builder()
                .nationalId(request.getNationalId())
                .password(encodedPassword)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .salary(request.getSalary())
                .address(request.getAddress())
                .maxTasks(request.getMaxTasks())
                .build();
    }

    public AlgoUserRequest toAlgoRequest(User user, int effectiveMaxTasks) {
        if (user == null) return null;

        List<AlgoVacationRequest> approvedVacations = user.getVacations() != null
                ? user.getVacations().stream()
                        .filter(v -> v.getStatus() != null
                                && VacationStatusLevel.APPROVED.name().equalsIgnoreCase(v.getStatus().getName()))
                        .map(v -> AlgoVacationRequest.builder()
                                .id(v.getId())
                                .startDate(v.getStartDate())
                                .endDate(v.getEndDate())
                                .build())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        List<WorkerAvailabilityDto> availabilities = user.getAvailabilities() != null
                ? user.getAvailabilities().stream()
                        .map(a -> WorkerAvailabilityDto.builder()
                                .id(a.getId())
                                .dayOfWeek(a.getDayOfWeek())
                                .startTime(a.getStartTime())
                                .endTime(a.getEndTime())
                                .build())
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return AlgoUserRequest.builder()
                .id(user.getId())
                .availabilities(availabilities)
                .maxTasks(effectiveMaxTasks)
                .skillIds(user.getSkills() != null
                        ? user.getSkills().stream().map(Skill::getId).collect(Collectors.toSet())
                        : Collections.emptySet())
                .vacations(approvedVacations)
                .build();
    }
}
