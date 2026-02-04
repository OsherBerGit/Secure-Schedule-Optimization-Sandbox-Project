package com.example.mainbackend.dto.user;

import lombok.Builder;
import lombok.Data;

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
    private Integer dailyAvailabilityHours;
    private Integer maxTasks;

    // Roles as role names (not full entity to avoid circular references)
    private Set<String> roles;
}
