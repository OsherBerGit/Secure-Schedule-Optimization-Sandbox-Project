package com.example.algorithm.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Represents a Worker as seen by the scheduling algorithm.
 *
 * <p>Zero-Trust compliant: no PII (names, emails) — only capacity and job data.
 * Pure Java: no Spring, Jackson, or Lombok annotations.</p>
 *
 * <p>Immutable by design — fields are set once via the constructor and exposed
 * through read-only getters to prevent accidental mutation inside the engine.</p>
 */
public final class AlgoUser {

    private final Long id;

    /** Specific weekly shift windows defining when this worker is available. */
    private final List<AlgoWorkerAvailability> availabilities;

    private final Integer maxTasks;

    /** Opaque job identifiers (stored as strings of job IDs) */
    private final Set<String> jobs;

    /** Approved vacation windows — treated as fully blocked availability */
    private final List<AlgoVacation> vacations;

    public AlgoUser(Long id,
                    List<AlgoWorkerAvailability> availabilities,
                    Integer maxTasks,
                    Set<String> jobs,
                    List<AlgoVacation> vacations) {
        this.id             = id;
        this.availabilities = availabilities != null
                ? Collections.unmodifiableList(availabilities)
                : Collections.emptyList();
        this.maxTasks       = maxTasks;
        this.jobs = jobs != null ? Collections.unmodifiableSet(jobs) : Collections.emptySet();
        this.vacations      = vacations != null ? Collections.unmodifiableList(vacations) : Collections.emptyList();
    }

    public Long getId()                                    { return id; }
    public List<AlgoWorkerAvailability> getAvailabilities() { return availabilities; }
    public Integer getMaxTasks()                           { return maxTasks; }
    public Set<String> getJobs()                          { return jobs; }
    public List<AlgoVacation> getVacations()               { return vacations; }

    @Override
    public String toString() {
        return "AlgoUser{id=" + id
                + ", availabilities=" + availabilities
                + ", maxTasks=" + maxTasks
                + ", jobs=" + jobs
                + ", vacations=" + vacations + '}';
    }
}
