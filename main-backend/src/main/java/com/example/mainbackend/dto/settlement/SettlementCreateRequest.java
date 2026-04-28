package com.example.mainbackend.dto.settlement;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementCreateRequest {

    @NotNull(message = "Task ID is required")
    private Long taskId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Settlement date is required")
    private LocalDateTime settlementDate;

    private LocalDateTime completionDate;

    private Long statusId;
}
