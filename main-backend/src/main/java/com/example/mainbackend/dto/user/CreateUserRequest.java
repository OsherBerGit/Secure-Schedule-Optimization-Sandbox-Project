package com.example.mainbackend.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateUserRequest {
    private String nationalId; // Israeli National ID (Teudat Zehut)
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Double salary;
    private String address;
    private Integer dailyAvailabilityHours;
    private Integer maxTasks;
}
