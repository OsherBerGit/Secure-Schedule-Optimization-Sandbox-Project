package com.example.mainbackend.mapper;

import com.example.mainbackend.dto.constrainttype.ConstraintTypeResponseDto;
import com.example.mainbackend.entity.ConstraintType;
import org.springframework.stereotype.Component;

@Component
public class ConstraintTypeMapper {

    public ConstraintTypeResponseDto toDto(ConstraintType constraintType) {
        if (constraintType == null) return null;

        return ConstraintTypeResponseDto.builder()
                .id(constraintType.getId())
                .name(constraintType.getName())
                .description(constraintType.getDescription())
                .build();
    }
}

