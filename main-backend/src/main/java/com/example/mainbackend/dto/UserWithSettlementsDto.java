package com.example.mainbackend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class UserWithSettlementsDto {
    private Long id;
    private String teudatZehut;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Double salary;

    // Roles as role names
    private Set<String> roles;

    // Settlement information
    private List<SettlementDto> settlements;

    @Data
    @Builder
    public static class SettlementDto {
        private Long id;
        private LocalDate settlementDate;
        private Double amount;
        private Double hoursWorked;
        private String description;
    }
}
