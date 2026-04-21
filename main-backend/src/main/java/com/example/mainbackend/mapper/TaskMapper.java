package com.example.mainbackend.mapper;

import com.example.mainbackend.algorithm.dto.AlgoConstraintRequest;
import com.example.mainbackend.algorithm.dto.AlgoTaskRequest;
import com.example.mainbackend.algorithm.dto.ConstraintType;
import com.example.mainbackend.dto.skill.SkillDto;
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
                .departmentName(task.getDepartment() != null ? task.getDepartment().getName() : null)
                .version(task.getVersion())
                .requiredSkillIds(task.getRequiredSkills() != null ? 
                        task.getRequiredSkills().stream().map(com.example.mainbackend.entity.Skill::getId).collect(Collectors.toSet()) : null)
                .requiredSkills(task.getRequiredSkills() != null ? 
                        task.getRequiredSkills().stream().map(s -> 
                            SkillDto.builder()
                                .id(s.getId())
                                .name(s.getName())
                                .build()
                        ).collect(Collectors.toSet()) : null)
                .build();
    }

    public AlgoTaskRequest toAlgoRequest(Task task, Set<Long> openTaskIds) {
        if (task == null) return null;

        return AlgoTaskRequest.builder()
                .id(task.getId())
                .durationHours(task.getDurationHours())
                .deadline(task.getDeadline())
                .priorityLevel(task.getPriority() != null ? task.getPriority().getValue() : null)
                .requiredSkillIds(task.getRequiredSkills() != null ? 
                        task.getRequiredSkills().stream().map(com.example.mainbackend.entity.Skill::getId).collect(Collectors.toSet()) : null)
                .constraints(mapConstraints(task.getIncomingConstraints(), openTaskIds))
                .build();
    }

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
