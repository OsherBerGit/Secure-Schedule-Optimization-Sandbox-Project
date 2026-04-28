package com.example.algorithm.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Represents a Task as seen by the scheduling algorithm.
public final class AlgoTask {

    private final Long id;
    private final Integer durationHours;
    private final LocalDateTime deadline;
    private final Integer priorityLevel;

    private final Set<Long> requiredSkills;

    private final List<AlgoConstraint> constraints;

    public AlgoTask(Long id, Integer durationHours, LocalDateTime deadline, Integer priorityLevel, Set<Long> requiredSkills, List<AlgoConstraint> constraints) {
        this.id = id;
        this.durationHours = durationHours;
        this.deadline = deadline;
        this.priorityLevel = priorityLevel != null ? priorityLevel : 0;
        this.requiredSkills = requiredSkills != null ? Collections.unmodifiableSet(requiredSkills) : Collections.emptySet();
        this.constraints = constraints != null ? Collections.unmodifiableList(constraints) : Collections.emptyList();
    }

    public Long getId() { return id; }
    public Integer getDurationHours() { return durationHours; }
    public LocalDateTime getDeadline() { return deadline; }
    public Integer getPriorityLevel() { return priorityLevel; }
    public Set<Long> getRequiredSkills() { return requiredSkills; }
    public List<AlgoConstraint> getConstraints() { return constraints; }

    public String getStatus() { return null; }

    @Override
    public String toString() {
        return "AlgoTask{id=" + id
                + ", durationHours=" + durationHours
                + ", deadline=" + deadline
                + ", priorityLevel=" + priorityLevel
                + ", requiredSkills=" + requiredSkills
                + ", predecessorTaskIds=" + constraints + '}';
    }
}
