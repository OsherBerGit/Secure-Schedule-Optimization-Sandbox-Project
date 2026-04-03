package com.example.mainbackend.service;

import com.example.mainbackend.dto.taskconstraint.TaskConstraintCreateRequest;
import com.example.mainbackend.dto.taskconstraint.TaskConstraintResponseDto;
import com.example.mainbackend.entity.ConstraintType;
import com.example.mainbackend.entity.Task;
import com.example.mainbackend.entity.TaskConstraint;
import com.example.mainbackend.mapper.TaskConstraintMapper;
import com.example.mainbackend.repository.ConstraintTypeRepository;
import com.example.mainbackend.repository.TaskConstraintRepository;
import com.example.mainbackend.repository.TaskRepository;
import com.example.mainbackend.security.SecurityHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskConstraintService {

    private final TaskConstraintRepository taskConstraintRepository;
    private final TaskRepository taskRepository;
    private final ConstraintTypeRepository constraintTypeRepository;
    private final TaskConstraintMapper mapper;
    private final SecurityHelper securityHelper;

    @Transactional
    public TaskConstraintResponseDto createConstraint(TaskConstraintCreateRequest request) {
        // Validate that predecessor and successor are different
        if (request.getPredecessorTaskId().equals(request.getSuccessorTaskId()))
            throw new IllegalArgumentException("A task cannot have a constraint with itself");

        // Check if constraint already exists
        if (taskConstraintRepository.existsByPredecessorTaskIdAndSuccessorTaskId(
                request.getPredecessorTaskId(), request.getSuccessorTaskId()))
            throw new IllegalArgumentException("Constraint already exists between these tasks");

        // Fetch entities
        Task predecessorTask = taskRepository.findById(request.getPredecessorTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Predecessor task not found with ID: " + request.getPredecessorTaskId()));

        Task successorTask = taskRepository.findById(request.getSuccessorTaskId())
                .orElseThrow(() -> new IllegalArgumentException("Successor task not found with ID: " + request.getSuccessorTaskId()));

        ConstraintType constraintType = constraintTypeRepository.findById(request.getConstraintTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Constraint type not found with ID: " + request.getConstraintTypeId()));

        // Validate for circular dependency BEFORE saving
        validateNoCircularDependency(request.getPredecessorTaskId(), request.getSuccessorTaskId());

        // Build and save the constraint
        TaskConstraint constraint = TaskConstraint.builder()
                .predecessorTask(predecessorTask)
                .successorTask(successorTask)
                .constraintType(constraintType)
                .lagMinutes(request.getLagMinutes() != null ? request.getLagMinutes() : 0)
                .build();

        TaskConstraint saved = taskConstraintRepository.save(constraint);
        return mapper.toDto(saved);
    }

    @Transactional
    public TaskConstraintResponseDto getConstraintById(Long id) {
        TaskConstraint constraint = taskConstraintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task constraint not found with ID: " + id));
        return mapper.toDto(constraint);
    }

    @Transactional
    public List<TaskConstraintResponseDto> getAllConstraints() {
        if (securityHelper.isManager()) {
            Long deptId = securityHelper.getCurrentUserDepartmentId();
            return taskConstraintRepository.findAll().stream()
                    .filter(c -> c.getPredecessorTask().getDepartment() != null
                              && c.getPredecessorTask().getDepartment().getId().equals(deptId))
                    .map(mapper::toDto)
                    .toList();
        }
        return taskConstraintRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public List<TaskConstraintResponseDto> getConstraintsByPredecessorTask(Long taskId) {
        return taskConstraintRepository.findByPredecessorTaskId(taskId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public List<TaskConstraintResponseDto> getConstraintsBySuccessorTask(Long taskId) {
        return taskConstraintRepository.findBySuccessorTaskId(taskId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public TaskConstraintResponseDto updateConstraint(Long id, TaskConstraintCreateRequest request) {
        TaskConstraint existing = taskConstraintRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task constraint not found with ID: " + id));

        // Validate self-reference
        if (request.getPredecessorTaskId().equals(request.getSuccessorTaskId()))
            throw new IllegalArgumentException("A task cannot have a constraint with itself");

        // If tasks are changing, validate circular dependency
        if (!existing.getPredecessorTask().getId().equals(request.getPredecessorTaskId()) ||
            !existing.getSuccessorTask().getId().equals(request.getSuccessorTaskId())) {

            // Temporarily remove this constraint from the graph for validation
            taskConstraintRepository.delete(existing);
            taskConstraintRepository.flush();

            validateNoCircularDependency(request.getPredecessorTaskId(), request.getSuccessorTaskId());

            // Re-fetch entities
            Task predecessorTask = taskRepository.findById(request.getPredecessorTaskId())
                    .orElseThrow(() -> new IllegalArgumentException("Predecessor task not found"));
            Task successorTask = taskRepository.findById(request.getSuccessorTaskId())
                    .orElseThrow(() -> new IllegalArgumentException("Successor task not found"));

            existing.setPredecessorTask(predecessorTask);
            existing.setSuccessorTask(successorTask);
        }

        // Update constraint type
        ConstraintType constraintType = constraintTypeRepository.findById(request.getConstraintTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Constraint type not found"));
        existing.setConstraintType(constraintType);

        // Update lag minutes
        existing.setLagMinutes(request.getLagMinutes() != null ? request.getLagMinutes() : 0);

        TaskConstraint updated = taskConstraintRepository.save(existing);

        return mapper.toDto(updated);
    }

    @Transactional
    public boolean deleteConstraint(Long id) {
        if (!taskConstraintRepository.existsById(id))
            throw new IllegalArgumentException("Task constraint not found with ID: " + id);

        taskConstraintRepository.deleteById(id);
        return true;
    }

    /**
     * Validates that adding a constraint from predecessorId to successorId
     * will not create a circular dependency in the task graph.
     *
     * <p>Uses DFS (Depth-First Search) to detect cycles.</p>
     *
     * <h3>Complexity Analysis</h3>
     * <ul>
     *   <li><b>Time Complexity:</b> O(V + E)</li>
     *   <li><b>Variables:</b>
     *     <ul>
     *       <li>V = Vertices (Tasks)</li>
     *       <li>E = Edges (Dependencies/Constraints)</li>
     *     </ul>
     *   </li>
     *   <li><b>Explanation:</b>
     *     <ul>
     *       <li>Building the graph takes O(E).</li>
     *       <li>The DFS traversal visits every node and edge at most once.</li>
     *       <li>We maintain a <code>visited</code> set to avoid recounting nodes and a <code>recursionStack</code> to detect back-edges (cycles) in the current path.</li>
     *       <li>This ensures linear time complexity relative to the size of the graph.</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    private void validateNoCircularDependency(Long predecessorId, Long successorId) {
        // Build adjacency list of the constraint graph
        Map<Long, List<Long>> graph = new HashMap<>();
        List<TaskConstraint> allConstraints = taskConstraintRepository.findAll();

        for (TaskConstraint constraint : allConstraints) {
            Long pred = constraint.getPredecessorTask().getId();
            Long succ = constraint.getSuccessorTask().getId();
            graph.computeIfAbsent(pred, k -> new ArrayList<>()).add(succ);
        }

        // Add the new edge
        graph.computeIfAbsent(predecessorId, k -> new ArrayList<>()).add(successorId);

        // Check if there's a cycle using DFS
        Set<Long> visited = new HashSet<>();
        Set<Long> recursionStack = new HashSet<>();

        for (Long node : graph.keySet())
            if (hasCycle(node, graph, visited, recursionStack))
                throw new IllegalArgumentException("Adding this constraint would create a circular dependency in the task graph");
    }

    /**
     * DFS cycle detection helper.
     */
    private boolean hasCycle(Long node, Map<Long, List<Long>> graph,
                             Set<Long> visited, Set<Long> recursionStack) {
        if (recursionStack.contains(node))
            return true; // Cycle detected

        if (visited.contains(node))
            return false; // Already processed

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
