package com.example.mainbackend.mapper;

import com.example.mainbackend.algorithm.dto.AlgoTaskRequest;
import com.example.mainbackend.dto.task.TaskResponseDto;
import com.example.mainbackend.entity.Role;
import com.example.mainbackend.entity.Task;
import com.example.mainbackend.entity.TaskConstraint;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TaskMapper {

    public TaskResponseDto toDto(Task task) {
        if (task == null) return null;

        return TaskResponseDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .deadline(task.getDeadline())
                .durationHours(task.getDurationHours())
                .startTime(task.getStartTime())
                .priorityId(task.getPriority() != null ? task.getPriority().getId() : null)
                .priorityName(task.getPriority() != null ? task.getPriority().getName() : null)
                .taskStatusId(task.getStatus() != null ? task.getStatus().getId() : null)
                .taskStatusName(task.getStatus() != null ? task.getStatus().getName() : null)
                .taskStatusColorCode(task.getStatus() != null ? task.getStatus().getColorCode() : null)
                .build();
    }

    /**
     * Maps a Task entity to an anonymous AlgoTaskRequest.
     * Zero-Trust: only scheduling-relevant fields are included — no titles, descriptions, or PII.
     *
     * @param task the Task entity (must have requiredRoles and incomingConstraints already loaded)
     * @return anonymous AlgoTaskRequest for the algorithm engine
     */
    public static AlgoTaskRequest toAlgoRequest(Task task) {
        return toAlgoRequest(task, null);
    }

    /**
     * Maps a Task entity to an anonymous AlgoTaskRequest, filtering out predecessor IDs
     * that are not present in {@code openTaskIds}.
     *
     * This prevents the algorithm from rejecting a task because its predecessor is already
     * SCHEDULED/LOCKED/CLOSED and therefore not included in the current scheduling run.
     * If a predecessor is not OPEN it has already been handled and the dependency is irrelevant.
     *
     * @param task        the Task entity (must have requiredRoles and incomingConstraints already loaded)
     * @param openTaskIds set of task IDs being sent in this scheduling request; pass
     *                    {@code null} to skip filtering (all predecessors are kept as-is)
     * @return anonymous AlgoTaskRequest for the algorithm engine
     */
    public static AlgoTaskRequest toAlgoRequest(Task task, Set<Long> openTaskIds) {
        if (task == null) return null;

        Set<Long> requiredRoleIds = task.getRequiredRoles() != null
                ? task.getRequiredRoles().stream().map(Role::getId).collect(Collectors.toSet())
                : Collections.emptySet();

        List<Long> predecessorIds = task.getIncomingConstraints() != null
                ? task.getIncomingConstraints().stream()
                        .map(TaskConstraint::getPredecessorTask)
                        .map(Task::getId)
                        .filter(predId -> openTaskIds == null || openTaskIds.contains(predId))
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return AlgoTaskRequest.builder()
                .id(task.getId())
                .durationHours(task.getDurationHours())
                .deadline(task.getDeadline())
                .priorityLevel(task.getPriority() != null ? task.getPriority().getValue() : null)
                .requiredRoles(requiredRoleIds)
                .predecessorTaskIds(predecessorIds)
                .build();
    }
}


