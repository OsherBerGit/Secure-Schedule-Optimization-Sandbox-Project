package com.example.mainbackend.constants;

import lombok.Getter;

@Getter
public enum TaskPriorityLevel {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int weight;

    TaskPriorityLevel(int weight) { this.weight = weight; }
}
