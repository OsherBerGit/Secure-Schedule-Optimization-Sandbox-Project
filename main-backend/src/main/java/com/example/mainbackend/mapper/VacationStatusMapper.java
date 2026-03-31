package com.example.mainbackend.mapper;

import com.example.mainbackend.dto.vacationstatus.VacationStatusResponseDto;
import com.example.mainbackend.entity.VacationStatus;
import org.springframework.stereotype.Component;

@Component
public class VacationStatusMapper {
    public VacationStatusResponseDto toDto(VacationStatus entity) {
        if (entity == null) return null;

        return VacationStatusResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
