package com.example.algorithm.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a Task as seen by the scheduling algorithm.
 *
 * <p>Zero-Trust compliant: no titles, descriptions, or status strings
 * only scheduling-relevant numeric and temporal data.
 * Pure Java: no Spring, Jackson, or Lombok annotations.</p>
 *
 * <p>Immutable by design fields are set once via the constructor and exposed
 * through read-only getters to prevent accidental mutation inside the engine.</p>
 *
 * <p>Field alignment with {@code TaskDto}:</p>
 * <ul>
 *   <li>{@code id}                direct copy from {@code TaskDto.id}</li>
 *   <li>{@code durationHours}     direct copy from {@code TaskDto.durationHours}</li>
 *   <li>{@code deadline}          direct copy from {@code TaskDto.deadline}</li>
 *   <li>{@code priorityLevel}     direct copy from {@code TaskDto.priorityLevel} (defaults to 0)</li>
 *   <li>{@code requiredSkills}       {@code TaskDto.requiredSkillId} converted to {@code Set<String>}</li>
 *   <li>{@code predecessorTaskIds} direct copy from {@code TaskDto.predecessorTaskIds}</li>
 * </ul>
 */
public final class AlgoTask {

    private final Long id;
    private final Integer durationHours;
    private final LocalDateTime deadline;
    private final Integer priorityLevel;

    /**
     * Opaque role identifiers required to perform this task.
     * Stored as Longs of skill IDs (e.g. 42L) for compatibility with AlgoUser.skills.
     */
    private final Set<Long> requiredSkills;

    private final List<AlgoConstraint> constraints;

    public AlgoTask(Long id,
                    Integer durationHours,
                    LocalDateTime deadline,
                    Integer priorityLevel,
                    Set<Long> requiredSkills,
                    List<AlgoConstraint> constraints) {
        this.id                 = id;
        this.durationHours      = durationHours;
        this.deadline           = deadline;
        this.priorityLevel      = priorityLevel != null ? priorityLevel : 0;
        this.requiredSkills = requiredSkills != null
                ? Collections.unmodifiableSet(requiredSkills)
                : Collections.emptySet();
        this.constraints = constraints != null
                ? Collections.unmodifiableList(constraints)
                : Collections.emptyList();
    }

    public Long getId()                      { return id; }
    public Integer getDurationHours()        { return durationHours; }
    public LocalDateTime getDeadline()       { return deadline; }
    public Integer getPriorityLevel()        { return priorityLevel; }
    public Set<Long> getRequiredSkills()    { return requiredSkills; }
    public List<AlgoConstraint> getConstraints(){ return constraints; }

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
                + ", requiredSkills=" + requiredSkills
                + ", predecessorTaskIds=" + constraints + '}';
    }
}
