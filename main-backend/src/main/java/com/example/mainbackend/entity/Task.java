package com.example.mainbackend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a work requirement in the system.
 * Lifecycle: OPEN → LOCKED (assigned by algorithm) → CLOSED (all settlements done).
 * Uses a manual Builder to allow default status injection at construction time.
 */

// TODO: Replace the manual implementation with JPA annotations and Lombok once the entity design is finalized and stable.
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
    private Priority priority;

    /**
     * Task lifecycle status (OPEN → LOCKED → CLOSED).
     * Stored in the task_statuses table.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_status_id", nullable = false)
    private TaskStatus status;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_required_roles",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> requiredRoles = new HashSet<>();

    @OneToMany(mappedBy = "predecessorTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskConstraint> outgoingConstraints = new ArrayList<>();

    @OneToMany(mappedBy = "successorTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskConstraint> incomingConstraints = new ArrayList<>();

    // ── Constructors ──────────────────────────────────────────────────────────

    public Task() {}

    private Task(Builder b) {
        this.id                 = b.id;
        this.title              = b.title;
        this.description        = b.description;
        this.durationHours      = b.durationHours;
        this.deadline           = b.deadline;
        this.priority           = b.priority;
        this.status             = b.status;
        this.startTime          = b.startTime;
        this.requiredRoles      = b.requiredRoles      != null ? b.requiredRoles      : new HashSet<>();
        this.outgoingConstraints= b.outgoingConstraints!= null ? b.outgoingConstraints: new ArrayList<>();
        this.incomingConstraints= b.incomingConstraints!= null ? b.incomingConstraints: new ArrayList<>();
    }

    // ── Manual Builder ────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private String title;
        private String description;
        private Integer durationHours;
        private LocalDateTime deadline;
        private Priority priority;
        private TaskStatus status;
        private LocalDateTime startTime;
        private Set<Role> requiredRoles;
        private List<TaskConstraint> outgoingConstraints;
        private List<TaskConstraint> incomingConstraints;

        private Builder() {}

        public Builder id(Long id)                                               { this.id = id; return this; }
        public Builder title(String title)                                       { this.title = title; return this; }
        public Builder description(String description)                           { this.description = description; return this; }
        public Builder durationHours(Integer durationHours)                      { this.durationHours = durationHours; return this; }
        public Builder deadline(LocalDateTime deadline)                           { this.deadline = deadline; return this; }
        public Builder priority(Priority priority)                                { this.priority = priority; return this; }
        public Builder status(TaskStatus status)                                  { this.status = status; return this; }
        public Builder startTime(LocalDateTime startTime)                         { this.startTime = startTime; return this; }
        public Builder requiredRoles(Set<Role> requiredRoles)                    { this.requiredRoles = requiredRoles; return this; }
        public Builder outgoingConstraints(List<TaskConstraint> outgoing)        { this.outgoingConstraints = outgoing; return this; }
        public Builder incomingConstraints(List<TaskConstraint> incoming)        { this.incomingConstraints = incoming; return this; }

        public Task build() { return new Task(this); }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                                      { return id; }
    public void setId(Long id)                               { this.id = id; }
    public String getTitle()                                 { return title; }
    public void setTitle(String title)                       { this.title = title; }
    public String getDescription()                           { return description; }
    public void setDescription(String description)           { this.description = description; }
    public Integer getDurationHours()                        { return durationHours; }
    public void setDurationHours(Integer durationHours)      { this.durationHours = durationHours; }
    public LocalDateTime getDeadline()                       { return deadline; }
    public void setDeadline(LocalDateTime deadline)          { this.deadline = deadline; }
    public Priority getPriority()                            { return priority; }
    public void setPriority(Priority priority)               { this.priority = priority; }
    public TaskStatus getStatus()                            { return status; }
    public void setStatus(TaskStatus status)                 { this.status = status; }
    public LocalDateTime getStartTime()                      { return startTime; }
    public void setStartTime(LocalDateTime startTime)        { this.startTime = startTime; }
    public Set<Role> getRequiredRoles()                      { return requiredRoles; }
    public void setRequiredRoles(Set<Role> requiredRoles)    { this.requiredRoles = requiredRoles; }
    public List<TaskConstraint> getOutgoingConstraints()     { return outgoingConstraints; }
    public void setOutgoingConstraints(List<TaskConstraint> l){ this.outgoingConstraints = l; }
    public List<TaskConstraint> getIncomingConstraints()     { return incomingConstraints; }
    public void setIncomingConstraints(List<TaskConstraint> l){ this.incomingConstraints = l; }
}
