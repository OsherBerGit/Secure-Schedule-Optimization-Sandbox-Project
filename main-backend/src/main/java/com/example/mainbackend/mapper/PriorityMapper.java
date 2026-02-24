package com.example.mainbackend.mapper;

import com.example.mainbackend.dto.priority.PriorityResponseDto;
import com.example.mainbackend.entity.Priority;
import org.springframework.stereotype.Component;

@Component
public class PriorityMapper {

    public PriorityResponseDto toDto(Priority priority) {
        if (priority == null) return null;

        return PriorityResponseDto.builder()
                .id(priority.getId())
                .name(priority.getName())
                .build();
    }
}
