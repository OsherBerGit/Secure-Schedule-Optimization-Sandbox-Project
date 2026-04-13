package com.example.mainbackend.constants;

import lombok.Getter;

@Getter
public enum TaskStatusLevel {
    OPEN,
    LOCKED,
    SCHEDULED,
    CLOSED
}
