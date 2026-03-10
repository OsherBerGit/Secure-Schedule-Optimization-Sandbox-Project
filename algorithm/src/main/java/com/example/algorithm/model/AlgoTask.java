package com.example.algorithm.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Represents a Task as seen by the scheduling algorithm.
 *
 * <p>Zero-Trust compliant: no titles, descriptions, or status strings —
 * only scheduling-relevant numeric and temporal data.
 * Pure Java: no Spring, Jackson, or Lombok annotations.</p>
 *
 * <p>Immutable by design — fields are set once via the constructor and exposed
 * through read-only getters to prevent accidental mutation inside the engine.</p>
 *
 * <p>Field alignment with {@code TaskDto}:</p>
 * <ul>
 *   <li>{@code id}                — direct copy from {@code TaskDto.id}</li>
 *   <li>{@code durationHours}     — direct copy from {@code TaskDto.durationHours}</li>
 *   <li>{@code deadline}          — direct copy from {@code TaskDto.deadline}</li>
 *   <li>{@code priorityLevel}     — direct copy from {@code TaskDto.priorityLevel} (defaults to 0)</li>
 *   <li>{@code requiredRoles}     — {@code TaskDto.requiredRoleIds} converted to {@code Set<String>}</li>
 *   <li>{@code predecessorTaskIds}— direct copy from {@code TaskDto.predecessorTaskIds}</li>
 * </ul>
 */
public final class AlgoTask {

    private final Long id;
    private final Integer durationHours;
    private final LocalDateTime deadline;
    private final Integer priorityLevel;

    /**
     * Opaque role identifiers required to perform this task.
     * Stored as Strings of role IDs (e.g. "42") for compatibility with AlgoUser.roles.
     */
    private final Set<String> requiredRoles;

    /** IDs of tasks that must complete before this task can start. */
    private final List<Long> predecessorTaskIds;

    public AlgoTask(Long id,
                    Integer durationHours,
                    LocalDateTime deadline,
                    Integer priorityLevel,
                    Set<String> requiredRoles,
                    List<Long> predecessorTaskIds) {
        this.id                 = id;
        this.durationHours      = durationHours;
        this.deadline           = deadline;
        this.priorityLevel      = priorityLevel != null ? priorityLevel : 0;
        this.requiredRoles      = requiredRoles != null
                ? Collections.unmodifiableSet(requiredRoles)
                : Collections.emptySet();
        this.predecessorTaskIds = predecessorTaskIds != null
                ? Collections.unmodifiableList(predecessorTaskIds)
                : Collections.emptyList();
    }

    public Long getId()                      { return id; }
    public Integer getDurationHours()        { return durationHours; }
    public LocalDateTime getDeadline()       { return deadline; }
    public Integer getPriorityLevel()        { return priorityLevel; }
    public Set<String> getRequiredRoles()    { return requiredRoles; }
    public List<Long> getPredecessorTaskIds(){ return predecessorTaskIds; }

    /**
     * Convenience alias kept for backward compatibility with BaseSchedulingStrategy,
     * which checks status to skip COMPLETED/CANCELLED tasks.
     * Since DTOs never carry status, this always returns null (engine will not skip it).
     */
    public String getStatus() { return null; }

    @Override
    public String toString() {
        return "AlgoTask{id=" + id
                + ", durationHours=" + durationHours
                + ", deadline=" + deadline
                + ", priorityLevel=" + priorityLevel
                + ", requiredRoles=" + requiredRoles
                + ", predecessorTaskIds=" + predecessorTaskIds + '}';
    }
}
