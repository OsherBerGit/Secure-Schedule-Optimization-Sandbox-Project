package com.example.mainbackend.mapper;

import com.example.mainbackend.dto.UserDto;
import com.example.mainbackend.entity.Role;
import com.example.mainbackend.entity.Settlement;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.entity.Vacation;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) return null;

        return UserDto.builder()
                .id(user.getId())
                .teudatZehut(user.getTeudatZehut())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .salary(user.getSalary())
                .address(user.getAddress())
                .dailyAvailabilityHours(user.getDailyAvailabilityHours())
                .maxTasks(user.getMaxTasks())
                .roles(user.getRoles() != null
                        ? user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet())
                        : Collections.emptySet())
                .vacationIds(user.getVacations() != null
                        ? user.getVacations().stream().map(Vacation::getId).collect(Collectors.toList())
                        : Collections.emptyList())
                .settlementIds(user.getSettlements() != null
                        ? user.getSettlements().stream().map(Settlement::getId).collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }

    public User toEntity(UserDto userDto) {
        if (userDto == null) return null;

        return User.builder()
                .id(userDto.getId())
                .teudatZehut(userDto.getTeudatZehut())
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .email(userDto.getEmail())
                .phoneNumber(userDto.getPhoneNumber())
                .salary(userDto.getSalary())
                .address(userDto.getAddress())
                .dailyAvailabilityHours(userDto.getDailyAvailabilityHours())
                .maxTasks(userDto.getMaxTasks())
                // Note: roles, vacations, settlements are not set here
                // They should be fetched from DB by service layer using the IDs/names
                .build();
    }
}
