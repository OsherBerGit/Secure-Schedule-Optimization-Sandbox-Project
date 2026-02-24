package com.example.mainbackend.mapper;

import com.example.mainbackend.dto.status.StatusResponseDto;
import com.example.mainbackend.entity.Status;
import org.springframework.stereotype.Component;

@Component
public class StatusMapper {

    public StatusResponseDto toDto(Status status) {
        if (status == null) return null;

        return StatusResponseDto.builder()
                .id(status.getId())
                .name(status.getName())
                .build();
    }
}

