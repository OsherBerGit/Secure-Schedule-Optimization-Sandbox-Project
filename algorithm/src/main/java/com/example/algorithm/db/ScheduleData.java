package com.example.algorithm.db;

import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;

import java.util.List;

/**
 * A snapshot of all data loaded from the database,
 * ready to be consumed by the scheduling algorithm.
 */
public record ScheduleData(
        List<AlgoUser> users,
        List<AlgoTask> tasks
) {}

