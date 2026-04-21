package com.example.mainbackend.mapper;

import com.example.mainbackend.dto.settlement.SettlementResponseDto;
import com.example.mainbackend.entity.Settlement;
import org.springframework.stereotype.Component;

@Component
public class SettlementMapper {

    public SettlementResponseDto toDto(Settlement settlement) {
        if (settlement == null) return null;

        return SettlementResponseDto.builder()
                .id(settlement.getId())
                .taskId(settlement.getTask().getId())
                .userId(settlement.getUser().getId())
                .settlementDate(settlement.getSettlementDate())
                .completionDate(settlement.getCompletionDate())
                .taskTitle(settlement.getTask().getTitle())
                .userName(settlement.getUser().getFirstName() + " " + settlement.getUser().getLastName())
                .statusId(settlement.getStatus() != null ? settlement.getStatus().getId() : null)
                .statusName(settlement.getStatus() != null ? settlement.getStatus().getName() : null)
                .build();
    }
}
