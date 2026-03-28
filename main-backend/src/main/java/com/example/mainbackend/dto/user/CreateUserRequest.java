package com.example.mainbackend.dto.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class CreateUserRequest {

    @NotBlank(message = "National ID is required")
    @Size(min = 1, max = 20, message = "National ID must be between 1 and 20 characters")
    private String nationalId; // Israeli National ID (Teudat Zehut)

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Pattern(regexp = "^[+]?[0-9\\-\\s]*$", message = "Phone number format is invalid")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phoneNumber;

    @PositiveOrZero(message = "Salary must be zero or positive")
    private Double salary;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    /** Weekly availability windows (shifts) for this worker. Optional at creation time. */
    @Valid
    private List<WorkerAvailabilityDto> availabilities;

    @Positive(message = "Max tasks must be positive")
    private Integer maxTasks;

    /**
     * Role to assign to the new user: ADMIN, MANAGER or WORKER.
     * Defaults to WORKER if not provided.
     */
    private String role;

    /**
     * Optional: List of Job names to assign to the user.
     */
    private Set<String> jobs;

    /** Optional: name of the department to assign this user to on creation. */
    private String departmentName;
}
