package com.example.mainbackend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Vacation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User worker;

    private LocalDate startDate;
    private LocalDate endDate;
}
