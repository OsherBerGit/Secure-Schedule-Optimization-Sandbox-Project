package com.example.mainbackend.dto.settlement;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SettlementCreateRequest {
    private Long taskId;
    private Long workerId;
    private LocalDateTime settlementDate;
    private LocalDateTime completionDate;
}
