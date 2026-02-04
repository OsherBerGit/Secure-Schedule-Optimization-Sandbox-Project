package com.example.mainbackend.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class UserWithVacationsDto {
    private Long id;
    private String nationalId; // Israeli National ID (Teudat Zehut)
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    // Roles as role names
    private Set<String> roles;

    // Vacation information
    private List<VacationDto> vacations;

    @Data
    @Builder
    public static class VacationDto {
        private Long id;
        private LocalDate startDate;
        private LocalDate endDate;
        private String reason;
        private String status; // e.g., PENDING, APPROVED, REJECTED
    }
}
