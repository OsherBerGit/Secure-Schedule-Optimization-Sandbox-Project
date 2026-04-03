package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a work requirement in the system.
 * Lifecycle: OPEN → LOCKED (assigned by algorithm) → CLOSED (all settlements done).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String title;
    private String description;
    private Integer durationHours;
    private LocalDateTime deadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "priority_id", nullable = false)
    private TaskPriority priority;

    /**
     * Task lifecycle status (OPEN → SCHEDULED → CLOSED).
     * Stored in the task_statuses table.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_status_id", nullable = false)
    private TaskStatus status;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * Version field for optimistic locking to handle concurrent updates safely.
     */
    @Version
    private Long version;
    
    /**
     * The department this task belongs to.
     * Used by scheduling scope: MANAGERs only see tasks in their own department.
     * Nullable — tasks not yet assigned to a department are visible to ADMINs only.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = true)
    private Skill requiredSkill;

    @OneToMany(mappedBy = "predecessorTask", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TaskConstraint> outgoingConstraints = new ArrayList<>();

    @OneToMany(mappedBy = "successorTask", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TaskConstraint> incomingConstraints = new ArrayList<>();
}
