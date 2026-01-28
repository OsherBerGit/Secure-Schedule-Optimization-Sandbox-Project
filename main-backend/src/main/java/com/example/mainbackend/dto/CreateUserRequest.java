package com.example.mainbackend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateUserRequest {
    private String teudatZehut;
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
