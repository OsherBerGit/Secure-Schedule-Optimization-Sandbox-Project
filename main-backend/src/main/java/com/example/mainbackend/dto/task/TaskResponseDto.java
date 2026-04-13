package com.example.mainbackend.dto.task;

import com.example.mainbackend.dto.skill.SkillDto;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponseDto {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private Integer durationHours;
    private LocalDateTime startTime;

    // Priority metadata
    private Long priorityId;
    private String priorityName;

    // Task lifecycle status (OPEN, LOCKED, CLOSED) — from task_statuses table
    private Long taskStatusId;
    private String taskStatusName;
    private String taskStatusColorCode;

    private String departmentName;

    /**
     * Optimistic Locking Version.
     * Sent to frontend so it can be returned during saves to prevent stale updates.
     */
    private Long version;

    /**
     * Skill required for the task.
     */
    private Set<Long> requiredSkillIds;
    private Set<SkillDto> requiredSkills;
}
