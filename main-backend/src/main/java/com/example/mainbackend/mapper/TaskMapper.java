package com.example.mainbackend.mapper;

import com.example.mainbackend.algorithm.dto.AlgoConstraintRequest;
import com.example.mainbackend.algorithm.dto.AlgoTaskRequest;
import com.example.mainbackend.algorithm.dto.ConstraintType;
import com.example.mainbackend.dto.task.TaskResponseDto;
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
                .departmentName(task.getDepartment() != null ? task.getDepartment().getName() : null)
                .version(task.getVersion())
                .requiredJobId(task.getRequiredJob() != null ? task.getRequiredJob().getId() : null)
                .requiredJobName(task.getRequiredJob() != null ? task.getRequiredJob().getName() : null)
                .build();
    }

    /**
     * Maps a Task entity to an anonymous AlgoTaskRequest.
     * Zero-Trust: only scheduling-relevant fields are included — no titles, descriptions, or PII.
     *
     * @param task the Task entity
     * @return anonymous AlgoTaskRequest for the algorithm engine
     */
    public AlgoTaskRequest toAlgoRequest(Task task) {
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
     * @param task        the Task entity
     * @param openTaskIds set of task IDs being sent in this scheduling request; pass
     *                    {@code null} to skip filtering (all predecessors are kept as-is)
     * @return anonymous AlgoTaskRequest for the algorithm engine
     */

    /**
     * Maps a Task entity to an anonymous AlgoTaskRequest, including detailed constraints (FS, SS, etc.).
     * Filters out constraints where the predecessor is not in {@code openTaskIds}.
     */
    public AlgoTaskRequest toAlgoRequest(Task task, Set<Long> openTaskIds) {
        if (task == null) return null;

        return AlgoTaskRequest.builder()
                .id(task.getId())
                .durationHours(task.getDurationHours())
                .deadline(task.getDeadline())
                .priorityLevel(task.getPriority() != null ? task.getPriority().getValue() : null)
                .requiredJobId(task.getRequiredJob() != null ? task.getRequiredJob().getId() : null)
                .constraints(mapConstraints(task.getIncomingConstraints(), openTaskIds))
                .build();
    }

    /**
     * Helper to map TaskConstraint entities to AlgoConstraintRequest DTOs.
     */
    private List<AlgoConstraintRequest> mapConstraints(List<TaskConstraint> entities, Set<Long> openTaskIds) {
        if (entities == null || entities.isEmpty())
            return Collections.emptyList();

        return entities.stream()
                .filter(c -> c.getPredecessorTask() != null)
                .filter(c -> openTaskIds == null || openTaskIds.contains(c.getPredecessorTask().getId()))
                .map(c -> AlgoConstraintRequest.builder()
                        .predecessorId(c.getPredecessorTask().getId())
                        .type(mapStringToConstraintType(c.getConstraintType().getName()))
                        .build())
                .collect(Collectors.toList());
    }

    private ConstraintType mapStringToConstraintType(String entityType) {
        if (entityType == null) return ConstraintType.FS;

        return switch (entityType) {
            case "START_TO_START" -> ConstraintType.SS;
            case "FINISH_TO_FINISH" -> ConstraintType.FF;
            case "START_TO_FINISH" -> ConstraintType.SF;
            default -> ConstraintType.FS; // FINISH_TO_START
        };
    }
}
