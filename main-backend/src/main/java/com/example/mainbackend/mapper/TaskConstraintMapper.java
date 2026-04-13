package com.example.mainbackend.mapper;

import com.example.mainbackend.dto.taskconstraint.TaskConstraintResponseDto;
import com.example.mainbackend.entity.TaskConstraint;
import org.springframework.stereotype.Component;

@Component
public class TaskConstraintMapper {

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

