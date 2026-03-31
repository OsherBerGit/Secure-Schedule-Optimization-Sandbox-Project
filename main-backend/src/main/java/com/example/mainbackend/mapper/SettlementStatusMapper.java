package com.example.mainbackend.mapper;

import com.example.mainbackend.dto.settlementstatus.SettlementStatusResponseDto;
import com.example.mainbackend.entity.SettlementStatus;
import org.springframework.stereotype.Component;

@Component
public class SettlementStatusMapper {
    /**
     * Maps a SettlementStatus entity to its corresponding Response DTO.
     */
    public SettlementStatusResponseDto toDto(SettlementStatus entity) {
        if (entity == null) return null;

        return SettlementStatusResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
