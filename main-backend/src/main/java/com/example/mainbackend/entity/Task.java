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

    // Relationships to management entities - LAZY fetch to prevent N+1

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "priority_id", nullable = false)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;

    // Assignment to employee

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User assignedEmployee;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_required_roles",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> requiredRoles = new HashSet<>();

    // Bidirectional relationship with TaskConstraint
    @OneToMany(mappedBy = "firstTask", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TaskConstraint> constraintsAsFirst = new ArrayList<>();

    @OneToMany(mappedBy = "secondTask", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TaskConstraint> constraintsAsSecond = new ArrayList<>();
}
