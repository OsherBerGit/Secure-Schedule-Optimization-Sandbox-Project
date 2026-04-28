package com.example.algorithm.model;

import java.time.LocalDate;

// Represents an approved vacation window for a worker.
public final class AlgoVacation {

    private final Long id;
    private final Long userId;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public AlgoVacation(Long id, Long userId, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }

    @Override
    public String toString() {
        return "AlgoVacation{id=" + id
                + ", userId=" + userId
                + ", startDate=" + startDate
                + ", endDate=" + endDate + '}';
    }
}
