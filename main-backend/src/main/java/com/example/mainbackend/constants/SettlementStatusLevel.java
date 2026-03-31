package com.example.mainbackend.constants;

import lombok.Getter;

/**
 * Settlement exec → stored in settlement_statuses table → used by SettlementStatus entity.
 */
@Getter
public enum SettlementStatusLevel {
    PENDING,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
