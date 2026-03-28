package com.example.mainbackend.mapper;

import com.example.mainbackend.algorithm.dto.AlgoUserRequest;
import com.example.mainbackend.algorithm.dto.AlgoVacationRequest;
import com.example.mainbackend.constants.VacationStatusConstants;
import com.example.mainbackend.dto.user.CreateUserRequest;
import com.example.mainbackend.dto.user.UserDto;
import com.example.mainbackend.dto.user.WorkerAvailabilityDto;
import com.example.mainbackend.entity.Job;
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
                .jobs(user.getJobs() != null
                        ? user.getJobs().stream().map(Job::getName).collect(Collectors.toSet())
                        : Collections.emptySet())
                .build();
    }

    /**
     * Updates an existing User entity with data from UserDto.
     * Used for PATCH/PUT operations to avoid manual field copying.
     * Note: Does not update roles, vacations, or settlements - they should be handled separately.
     *
     * @param existingUser the User entity to update
     * @param userDto the source DTO with new values
     */
    public void updateEntityFromDto(User existingUser, UserDto userDto) {
        if (existingUser == null || userDto == null) return;

        existingUser.setNationalId(userDto.getNationalId());
        existingUser.setFirstName(userDto.getFirstName());
        existingUser.setLastName(userDto.getLastName());
        existingUser.setEmail(userDto.getEmail());
        existingUser.setPhoneNumber(userDto.getPhoneNumber());
        existingUser.setSalary(userDto.getSalary());
        existingUser.setAddress(userDto.getAddress());
        existingUser.setMaxTasks(userDto.getMaxTasks());
        // Note: roles, vacations, settlements, availabilities are not updated here
        // They should be handled separately by dedicated service methods
    }

    /**
     * Converts UserDto to User entity.
     * Note: This creates a basic entity without roles, vacations, or settlements.
     * These relationships should be set by the service layer after fetching from DB.
     *
     * @param userDto the source DTO
     * @return User entity with basic fields populated, or null if input is null
     */
    public User toEntity(UserDto userDto) {
        if (userDto == null) return null;

        return User.builder()
                .id(userDto.getId())
                .nationalId(userDto.getNationalId())
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .email(userDto.getEmail())
                .phoneNumber(userDto.getPhoneNumber())
                .salary(userDto.getSalary())
                .address(userDto.getAddress())
                .maxTasks(userDto.getMaxTasks())
                // Note: roles, vacations, settlements are not set here
                // They should be fetched from DB by service layer using the IDs/names
                .build();
    }

    /**
     * Creates a User entity from CreateUserRequest.
     * Eliminates code duplication in service layer.
     * Note: Password should be encoded by the service layer before calling this method.
     *
     * @param request the creation request
     * @param encodedPassword the already-encoded password
     * @return User entity ready to be saved
     */
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
        // Note: availabilities from the request are not mapped here because they require
        // back-references to the saved User entity. The service layer should save the User
        // first, then persist the WorkerAvailability entries with the correct user_id.
    }

    /**
     * Maps a User entity to an anonymous AlgoUserRequest.
     * Zero-Trust: only scheduling-relevant IDs and capacity data are included.
     * No names, nationalIds, emails, or any PII are transmitted to the algorithm engine.
     *
     * Vacations are filtered to APPROVED status only — pending/rejected vacations
     * are irrelevant to the scheduling engine's availability calculation.
     *
     * @param user the User entity (must have roles already loaded)
     * @return anonymous AlgoUserRequest for the algorithm engine
     */
    public AlgoUserRequest toAlgoRequest(User user) {
        if (user == null) return null;

        List<AlgoVacationRequest> approvedVacations = user.getVacations() != null
                ? user.getVacations().stream()
                        .filter(v -> v.getStatus() != null
                                && VacationStatusConstants.APPROVED.equalsIgnoreCase(v.getStatus().getName()))
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
                .maxTasks(user.getMaxTasks())
                .jobIds(user.getJobs() != null
                        ? user.getJobs().stream().map(Job::getId).collect(Collectors.toSet())
                        : Collections.emptySet())
                .vacations(approvedVacations)
                .build();
    }
}
