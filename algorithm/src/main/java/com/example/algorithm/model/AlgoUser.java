package com.example.algorithm.model;

import java.util.Collections;
import java.util.List;
import java.util.Set;

// Represents a Worker as seen by the scheduling algorithm.
public final class AlgoUser {

    private final Long id;
    private final List<AlgoWorkerAvailability> availabilities;
    private final Integer maxTasks;
    private final Set<Long> skills;
    private final List<AlgoVacation> vacations;

    public AlgoUser(Long id, List<AlgoWorkerAvailability> availabilities, Integer maxTasks, Set<Long> skills, List<AlgoVacation> vacations) {
        this.id = id;
        this.availabilities = availabilities != null ? Collections.unmodifiableList(availabilities) : Collections.emptyList();
        this.maxTasks  = maxTasks;
        this.skills = skills != null ? Collections.unmodifiableSet(skills) : Collections.emptySet();
        this.vacations = vacations != null ? Collections.unmodifiableList(vacations) : Collections.emptyList();
    }

    public Long getId() { return id; }
    public List<AlgoWorkerAvailability> getAvailabilities() { return availabilities; }
    public Integer getMaxTasks() { return maxTasks; }
    public Set<Long> getSkills() { return skills; }
    public List<AlgoVacation> getVacations() { return vacations; }

    @Override
    public String toString() {
        return "AlgoUser{id=" + id
                + ", availabilities=" + availabilities
                + ", maxTasks=" + maxTasks
                + ", skills=" + skills
                + ", vacations=" + vacations + '}';
    }
}
