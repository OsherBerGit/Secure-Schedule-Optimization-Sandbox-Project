package com.example.mainbackend.dto.user;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

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

    @Positive(message = "Daily availability hours must be positive")
    @Max(value = 24, message = "Daily availability hours cannot exceed 24")
    private Integer dailyAvailabilityHours;

    @Positive(message = "Max tasks must be positive")
    private Integer maxTasks;
}
