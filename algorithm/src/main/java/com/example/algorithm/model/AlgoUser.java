package com.example.algorithm.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Represents a Worker as seen by the scheduling algorithm.
 *
 * <p>Zero-Trust compliant: no PII (names, emails) — only capacity and role data.
 * Pure Java: no Spring, Jackson, or Lombok annotations.</p>
 *
 * <p>Immutable by design — fields are set once via the constructor and exposed
 * through read-only getters to prevent accidental mutation inside the engine.</p>
 */
public final class AlgoUser {

    private final Long id;
    private final Integer dailyAvailabilityHours;
    private final Integer maxTasks;

    /** Opaque role identifiers (stored as strings of role IDs) */
    private final Set<String> roles;

    /** Approved vacation windows — treated as fully blocked availability */
    private final List<AlgoVacation> vacations;

    public AlgoUser(Long id,
                    Integer dailyAvailabilityHours,
                    Integer maxTasks,
                    Set<String> roles,
                    List<AlgoVacation> vacations) {
        this.id                    = id;
        this.dailyAvailabilityHours = dailyAvailabilityHours;
        this.maxTasks              = maxTasks;
        this.roles                 = roles != null ? Collections.unmodifiableSet(roles) : Collections.emptySet();
        this.vacations             = vacations != null ? Collections.unmodifiableList(vacations) : Collections.emptyList();
    }

    public Long getId()                      { return id; }
    public Integer getDailyAvailabilityHours() { return dailyAvailabilityHours; }
    public Integer getMaxTasks()             { return maxTasks; }
    public Set<String> getRoles()            { return roles; }
    public List<AlgoVacation> getVacations() { return vacations; }

    @Override
    public String toString() {
        return "AlgoUser{id=" + id
                + ", dailyAvailabilityHours=" + dailyAvailabilityHours
                + ", maxTasks=" + maxTasks
                + ", roles=" + roles
                + ", vacations=" + vacations + '}';
    }
}
