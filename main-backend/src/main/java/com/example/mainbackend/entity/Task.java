package com.example.mainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_status_id", nullable = false)
    private TaskStatus status;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    // Version field for optimistic locking to handle concurrent updates safely.
    @Version
    private Long version;
    
    /**
     * The department this task belongs to.
     * Used by scheduling scope: MANAGERs only see tasks in their own department.
     * Nullable - tasks not yet assigned to a department are visible to ADMINs only.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // Functional Skills: A task can require multiple Skill Titles (many-to-many)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_skills",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "skill_id"})
    )
    @Builder.Default
    private Set<Skill> requiredSkills = new HashSet<>();

    @OneToMany(mappedBy = "predecessorTask", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TaskConstraint> outgoingConstraints = new ArrayList<>();

    @OneToMany(mappedBy = "successorTask", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TaskConstraint> incomingConstraints = new ArrayList<>();
}
