package com.example.mainbackend.constants;

import lombok.Getter;

@Getter
public enum SettlementStatusLevel {
    PENDING,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}