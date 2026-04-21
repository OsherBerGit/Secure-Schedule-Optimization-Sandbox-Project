package com.example.mainbackend.dto.settlement;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponseDto {
    private Long id;
    private Long taskId;
    private Long userId;
    private LocalDateTime settlementDate;
    private LocalDateTime completionDate;

    // Display names for UI
    private String taskTitle;
    private String userName;

    // Settlement execution status (PENDING, IN_PROGRESS, COMPLETED, FAILED) — from settlement_statuses table
    private Long statusId;
    private String statusName;
}
