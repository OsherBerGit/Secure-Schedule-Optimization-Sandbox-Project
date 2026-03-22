package com.example.algorithm.engine;

import com.example.algorithm.model.ScheduleData;
import com.example.algorithm.model.*;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConstraintProgrammingStrategyTest {

    @Test
    void testSimpleSchedule() {
        // Create 1 User available all week
        AlgoWorkerAvailability availability = new AlgoWorkerAvailability(
                DayOfWeek.MONDAY, LocalTime.of(0, 0), LocalTime.of(23, 59)
        );
        AlgoUser user = new AlgoUser(1L, List.of(availability), 5, Set.of("WORKER"), Collections.emptyList());

        // Create 1 Task
        AlgoTask task = new AlgoTask(101L, 2, LocalDateTime.now().plusDays(2), 1, Set.of("WORKER"), Collections.emptyList());

        ScheduleData data = new ScheduleData(List.of(user), List.of(task));

        ConstraintProgrammingStrategy strategy = new ConstraintProgrammingStrategy();
        List<TaskAssignment> results = strategy.schedule(data);

        assertNotNull(results);
        assertEquals(1, results.size());
        TaskAssignment assignment = results.get(0);
        assertNotNull(assignment.getAssignedEmployee(), "Task should be assigned");
        assertEquals(1L, assignment.getAssignedEmployee().getId());
    }
}

