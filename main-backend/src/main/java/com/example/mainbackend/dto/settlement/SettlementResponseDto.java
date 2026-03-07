package com.example.mainbackend.dto.settlement;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SettlementResponseDto {
    private Long id;
    private Long taskId;
    private Long workerId;
    private LocalDateTime settlementDate;
    private LocalDateTime completionDate;

    // Display names for UI
    private String taskTitle;
    private String workerName;

    // Settlement execution status (PENDING, IN_PROGRESS, COMPLETED, FAILED) — from settlement_statuses table
    private Long statusId;
    private String statusName;
    private String statusColorCode;
}
