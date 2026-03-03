package com.example.algorithm;

import com.example.algorithm.db.DatabaseReader;
import com.example.algorithm.db.ScheduleData;
import com.example.algorithm.engine.GreedySchedulingStrategy;
import com.example.algorithm.engine.RoundRobinSchedulingStrategy;
import com.example.algorithm.engine.Scheduler;
import com.example.algorithm.model.TaskAssignment;

import java.util.List;

/**
 * Entry point for the algorithm module.
 * Loads data from DB and runs both scheduling strategies for comparison.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=== Schedule Optimization Algorithm ===\n");

        DatabaseReader reader = new DatabaseReader();
        ScheduleData data = reader.loadAll();

        System.out.println("Loaded: " + data.users().size() + " users, " + data.tasks().size() + " tasks\n");

        Scheduler scheduler = new Scheduler(new GreedySchedulingStrategy());

        // --- Run Greedy ---
        printResults(scheduler.run(data));

        System.out.println("\n" + "=".repeat(60) + "\n");

        // --- Switch to Round-Robin ---
        scheduler.setStrategy(new RoundRobinSchedulingStrategy());
        printResults(scheduler.run(data));
    }

    private static void printResults(List<TaskAssignment> assignments) {
        for (TaskAssignment a : assignments) {
            String employee = a.getAssignedEmployee() != null
                    ? a.getAssignedEmployee().getFirstName() + " " + a.getAssignedEmployee().getLastName()
                    : "UNASSIGNED";
            System.out.printf("  [Task %-2d] %-28s -> %-18s | %s -> %s | %s%n",
                    a.getTask().getId(),
                    a.getTask().getTitle(),
                    employee,
                    a.getScheduledStart().toLocalDate(),
                    a.getScheduledEnd().toLocalDate(),
                    a.getReason());
        }
    }
}
