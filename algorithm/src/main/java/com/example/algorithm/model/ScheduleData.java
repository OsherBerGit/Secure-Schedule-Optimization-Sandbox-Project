package com.example.algorithm.model;

import java.util.List;

public record ScheduleData(
        List<AlgoUser> users,
        List<AlgoTask> tasks
) {}

