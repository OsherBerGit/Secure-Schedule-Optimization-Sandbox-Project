package com.example.mainbackend.util;

import com.example.mainbackend.entity.TaskConstraint;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CycleDetectionUtil {

    // Helper to build an adjacency list representation of the constraints.
    public Map<Long, List<Long>> buildGraph(List<TaskConstraint> allConstraints) {
        Map<Long, List<Long>> graph = new HashMap<>();
        for (TaskConstraint constraint : allConstraints) {
            Long pred = constraint.getPredecessorTask().getId();
            Long succ = constraint.getSuccessorTask().getId();
            graph.computeIfAbsent(pred, k -> new ArrayList<>()).add(succ);
        }
        return graph;
    }

    /**
     * Checks if adding a constraint from predecessorId to successorId
     * will create a circular dependency in the task graph.
     *
     * <p>Uses DFS (Depth-First Search) to detect cycles.</p>
     *
     * <h3>Complexity Analysis</h3>
     * <ul>
     *   <li><b>Time Complexity:</b> O(V + E) in the worst case, where V is the number of tasks (Vertices)
     *       and E is the number of constraints (Edges). The DFS visits every node and edge at most once.</li>
     *   <li><b>Space Complexity:</b> O(V) for the recursion stack and visited sets, representing the maximum
     *       depth of the recursive call stack.</li>
     * </ul>
     *
     * @param predecessorId The task ID representing the source of the new hypothetical dependency.
     * @param successorId The task ID representing the target of the new hypothetical dependency.
     * @param graph The pre-built adjacency list representing the current task constraints.
     * @return true if adding the constraint creates a cycle, false otherwise.
     */
    public boolean wouldCreateCycle(Long predecessorId, Long successorId, Map<Long, List<Long>> graph) {
        // Add the new edge
        graph.computeIfAbsent(predecessorId, k -> new ArrayList<>()).add(successorId);

        // Check if there's a cycle using DFS
        Set<Long> visited = new HashSet<>();
        Set<Long> recursionStack = new HashSet<>();

        boolean cycleFound = false;
        for (Long node : graph.keySet()) {
            if (hasCycle(node, graph, visited, recursionStack)) {
                cycleFound = true;
                break;
            }
        }

        // Revert the hypothetical edge so the graph can be reused safely
        List<Long> edges = graph.get(predecessorId);
        if (edges != null) {
            edges.remove(successorId);
            if (edges.isEmpty())
                graph.remove(predecessorId);
        }

        return cycleFound;
    }

    // DFS cycle detection helper.
    private boolean hasCycle(Long node, Map<Long, List<Long>> graph, Set<Long> visited, Set<Long> recursionStack) {

        if (recursionStack.contains(node)) return true; // Cycle detected

        if (visited.contains(node)) return false; // Already processed

        visited.add(node);
        recursionStack.add(node);

        List<Long> neighbors = graph.get(node);
        if (neighbors != null)
            for (Long neighbor : neighbors)
                if (hasCycle(neighbor, graph, visited, recursionStack))
                    return true;

        recursionStack.remove(node);
        return false;
    }
}
