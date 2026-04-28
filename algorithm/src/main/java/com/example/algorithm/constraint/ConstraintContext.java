package com.example.algorithm.constraint;

import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;
import com.example.algorithm.model.TaskAssignment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ConstraintContext(
        AlgoTask task,
        AlgoUser candidate,
        LocalDateTime proposedStart,
        LocalDateTime proposedEnd,
        Map<Long, LocalDateTime> completionTimes,
        Map<Long, Integer> assignedCount,
        List<TaskAssignment> currentAssignments
) {}

