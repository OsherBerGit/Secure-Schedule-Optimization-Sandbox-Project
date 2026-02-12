package com.example.mainbackend.mapper;

import com.example.mainbackend.dto.taskconstraint.TaskConstraintResponseDto;
import com.example.mainbackend.entity.TaskConstraint;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting TaskConstraint entities to DTOs.
 */
@Component
public class TaskConstraintMapper {

    /**
     * Convert TaskConstraint entity to ResponseDto.
     * Includes related entity names for UI display.
     */
    public TaskConstraintResponseDto toDto(TaskConstraint constraint) {
        if (constraint == null) return null;

        return TaskConstraintResponseDto.builder()
                .id(constraint.getId())
                .predecessorTaskId(constraint.getPredecessorTask().getId())
                .successorTaskId(constraint.getSuccessorTask().getId())
                .constraintTypeId(constraint.getConstraintType().getId())
                .lagMinutes(constraint.getLagMinutes())
                .predecessorTaskTitle(constraint.getPredecessorTask().getTitle())
                .successorTaskTitle(constraint.getSuccessorTask().getTitle())
                .constraintTypeName(constraint.getConstraintType().getName())
                .build();
    }
}

