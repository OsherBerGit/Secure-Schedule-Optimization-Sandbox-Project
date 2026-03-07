package com.example.mainbackend.algorithm.repository;

import com.example.mainbackend.algorithm.entity.SchedulingConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SchedulingConfigurationRepository extends JpaRepository<SchedulingConfiguration, Long> {
    Optional<SchedulingConfiguration> findByIsActiveTrue();

    /**
     * Deactivates all configurations in a single bulk UPDATE.
     *
     * clearAutomatically = true  — evicts all managed entities from the 1st-level
     *   persistence context after the JPQL UPDATE executes. Without this, any
     *   SchedulingConfiguration already loaded in the same session would still
     *   show isActive=true in memory even though the DB row was updated, causing
     *   a stale-read bug on the subsequent save().
     *
     * flushAutomatically = true  — ensures any pending dirty entities are flushed
     *   to the DB BEFORE the bulk UPDATE runs, preventing lost-update scenarios
     *   where an in-memory change would be overwritten by the JPQL.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SchedulingConfiguration s SET s.isActive = false")
    void deactivateAll();
}
