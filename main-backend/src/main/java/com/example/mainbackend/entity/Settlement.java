package com.example.mainbackend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    private LocalDateTime settlementDate;
    private LocalDateTime completionDate;
}
