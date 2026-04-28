package com.example.algorithm.engine;

import com.example.algorithm.model.AlgoConstraint;
import com.example.algorithm.model.AlgoTask;

import java.util.*;

public class TopologicalSorter {

    // Sorts tasks topologically based on their dependencies.
    // Ties are broken using the provided priorityComparator.
    // Throws an exception if a circular dependency (cycle) is detected.
    public List<AlgoTask> sort(List<AlgoTask> tasks, Comparator<AlgoTask> priorityComparator) {
        Map<Long, AlgoTask> taskMap = new HashMap<>();
        Map<Long, Integer> inDegree = new HashMap<>();
        Map<Long, List<Long>> graph = new HashMap<>(); // Predecessor ID -> List of Successor IDs

        // 1. Initialize the graph and in-degree map
        for (AlgoTask task : tasks) {
            taskMap.put(task.getId(), task);
            inDegree.put(task.getId(), 0);
            graph.put(task.getId(), new ArrayList<>());
        }

        // 2. Build the graph and calculate in-degrees
        for (AlgoTask task : tasks) {
            if (task.getConstraints() != null && !task.getConstraints().isEmpty()) {
                for (AlgoConstraint constraint : task.getConstraints()) {
                    Long predId = constraint.predecessorId();
                    if (graph.containsKey(predId)) {
                        graph.get(predId).add(task.getId());
                        inDegree.put(task.getId(), inDegree.get(task.getId()) + 1);
                    }
                }
            }
        }

        // 3. Find all nodes with in-degree 0 and add to PriorityQueue
        // (This replaces the simple LinkedList Queue)
        PriorityQueue<AlgoTask> queue = new PriorityQueue<>(priorityComparator);
        for (Map.Entry<Long, Integer> entry : inDegree.entrySet())
            if (entry.getValue() == 0)
                queue.add(taskMap.get(entry.getKey()));

        // 4. Kahn's Algorithm (Priority-Aware)
        List<AlgoTask> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            // Poll returns the task with the highest priority among those with 0 dependencies
            AlgoTask currentTask = queue.poll();
            sorted.add(currentTask);
            Long currentId = currentTask.getId();

            for (Long neighborId : graph.get(currentId)) {
                int newDegree = inDegree.get(neighborId) - 1;
                inDegree.put(neighborId, newDegree);
                if (newDegree == 0)
                    queue.add(taskMap.get(neighborId));
            }
        }

        // 5. Check if the graph is acyclic
        if (sorted.size() != tasks.size())
            throw new IllegalStateException("Circular dependency detected in tasks! A valid schedule cannot be generated.");

        return sorted;
    }
}
