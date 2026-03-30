package com.example.sidebackend.service;

import com.example.algorithm.model.*;
import com.example.sidebackend.dto.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AlgoMapper — stateless translator between the side-backend DTO layer and the
 * algorithm model layer.
 *
 * <h3>Zero-Trust guarantee</h3>
 * <ul>
 * <li>No PII fields (names, emails) are ever copied — the DTOs don't carry them
 * and the models don't accept them.</li>
 * <li>Every {@code null} collection is normalised to an empty, immutable collection
 * so the algorithm engine never needs to perform null checks on lists/sets.</li>
 * </ul>
 *
 * <h3>Job ID convention</h3>
 * Both {@code UserDto.jobIds} and {@code TaskDto.requiredJobId} are processed as technical IDs.
 * {@code AlgoUser.jobs} and {@code AlgoTask.requiredJob} remain {@code Set<String>} in the engine.
 * The mapper converts each {@code Long} ID to its {@code String} representation (e.g. {@code 42L → "42"})
 * to facilitate high-performance {@code Set.contains()} checks during scheduling.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * AlgoUser  user   = AlgoMapper.toModel(userDto);
 * AlgoTask  task   = AlgoMapper.toModel(taskDto);
 * // Full request mapping:
 * MappedRequest mapped = AlgoMapper.toModels(request);
 * }</pre>
 */
@Component
public final class AlgoMapper {

    // ── VacationDto → AlgoVacation ────────────────────────────────────────────

    /**
     * Converts a single {@link VacationDto} belonging to the given worker.
     *
     * @param dto    source vacation record (must not be null)
     * @param userId the ID of the owning worker (for traceability inside the engine)
     * @return immutable {@link AlgoVacation}
     */
    public AlgoVacation toModel(VacationDto dto, Long userId) {
        return new AlgoVacation(
                dto.id(),
                userId,
                dto.startDate(),
                dto.endDate()
        );
    }

    /**
     * Converts a (possibly null) list of {@link VacationDto}s for one worker.
     * Returns an empty list when the input is null or empty.
     */
    public List<AlgoVacation> toVacationModels(List<VacationDto> dtos, Long userId) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        List<AlgoVacation> result = new ArrayList<>(dtos.size());
        for (VacationDto dto : dtos)
            result.add(toModel(dto, userId));
        return result;
    }

    // ── WorkerAvailabilityDto → AlgoWorkerAvailability ────────────────────────

    /**
     * Converts a single inlined {@link UserDto.WorkerAvailabilityDto} to an
     * {@link AlgoWorkerAvailability} model.
     */
    public AlgoWorkerAvailability toModel(UserDto.WorkerAvailabilityDto dto) {
        return new AlgoWorkerAvailability(dto.dayOfWeek(), dto.startTime(), dto.endTime());
    }

    /**
     * Converts a (possibly null) list of availability windows.
     * Returns an empty list when the input is null or empty.
     */
    public List<AlgoWorkerAvailability> toAvailabilityModels(List<UserDto.WorkerAvailabilityDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        List<AlgoWorkerAvailability> result = new ArrayList<>(dtos.size());
        for (UserDto.WorkerAvailabilityDto dto : dtos)
            result.add(toModel(dto));
        return result;
    }

    // ── UserDto → AlgoUser ────────────────────────────────────────────────────

    /**
     * Converts a single {@link UserDto} to an {@link AlgoUser} model.
     * Vacations are read directly from {@code dto.vacations()}.
     * Availability windows are mapped from {@code dto.availabilities()}.
     *
     * @param dto source DTO (must not be null)
     * @return immutable {@link AlgoUser}
     */
    public AlgoUser toModel(UserDto dto) {
        Set<String> jobs = jobIdsToStrings(dto.jobIds());
        List<AlgoVacation> vacations = toVacationModels(dto.vacations(), dto.id());
        List<AlgoWorkerAvailability> availabilities = toAvailabilityModels(dto.availabilities());
        return new AlgoUser(dto.id(), availabilities, dto.maxTasks(), jobs, vacations);
    }

    /**
     * Bulk-converts a list of {@link UserDto}s.
     * Returns an empty list when the input is null or empty.
     */
    public List<AlgoUser> toUserModels(List<UserDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        List<AlgoUser> result = new ArrayList<>(dtos.size());
        for (UserDto dto : dtos)
            result.add(toModel(dto));
        return result;
    }

    // ── TaskDto → AlgoTask ────────────────────────────────────────────────────

    /**
     * Converts a single {@link TaskDto} to an {@link AlgoTask} model.
     * A null {@code priorityLevel} is normalised to {@code 0} inside {@link AlgoTask}.
     *
     * @param dto source DTO (must not be null)
     * @return immutable {@link AlgoTask}
     */
    public AlgoTask toModel(TaskDto dto) {
        Set<String> requiredJobs = (dto.requiredJobId() != null)
                ? Collections.singleton(dto.requiredJobId().toString())
                : Collections.emptySet();

        List<AlgoConstraint> constraints = dto.constraints() != null
                ? dto.constraints().stream()
                  .map(this::toConstraintModel)
                  .filter(java.util.Objects::nonNull)
                  .toList()
                : Collections.emptyList();

        return new AlgoTask(
                dto.id(),
                dto.durationHours(),
                dto.deadline(),
                dto.priorityLevel(),
                requiredJobs,
                constraints
        );
    }

    /**
     * Bulk-converts a list of {@link TaskDto}s.
     * Returns an empty list when the input is null or empty.
     */
    public List<AlgoTask> toTaskModels(List<TaskDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        List<AlgoTask> result = new ArrayList<>(dtos.size());
        for (TaskDto dto : dtos)
            result.add(toModel(dto));
        return result;
    }

    /**
     * Converts a single {@link TaskConstraintDto} to an {@link AlgoConstraint} model.
     * Safely maps the DTO Enum to the Algorithm Model Enum.
     */
    private com.example.algorithm.model.AlgoConstraint toConstraintModel(TaskConstraintDto dto) {
        if (dto == null) return null;

        com.example.algorithm.model.ConstraintType modelType = com.example.algorithm.model.ConstraintType.FS; // Default

        if (dto.type() != null) {
            try {
                modelType = com.example.algorithm.model.ConstraintType.valueOf(dto.type().name());
            } catch (IllegalArgumentException e) { }
        }

        return new com.example.algorithm.model.AlgoConstraint(dto.predecessorId(), modelType);
    }

    // ── SchedulingConfigurationDto → AlgoSchedulingConfiguration ─────────────

    /**
     * Converts a {@link SchedulingConfigurationDto} to an
     * {@link AlgoSchedulingConfiguration} model.
     * Returns {@code null} when the input is {@code null} (config is optional in the request).
     *
     * @param dto source config DTO (may be null)
     * @return immutable {@link AlgoSchedulingConfiguration}, or {@code null} if dto is null
     */
    public AlgoSchedulingConfiguration toModel(SchedulingConfigurationDto dto) {
        if (dto == null) return null;
        return new AlgoSchedulingConfiguration(
            dto.weightPriority(),
            dto.weightDeadline(),
            dto.weightFairness(),
            dto.populationSize(),
            dto.maxGenerations(),
            dto.mutationRate(),
            dto.crossoverRate(),
            dto.localSearchFrequency()
        );
    }

    // ── Full request convenience method ───────────────────────────────────────

    /**
     * Extracts and converts all three model groups from a {@link SchedulingRequestDto}
     * at once, returning them as a {@link MappedRequest} value object.
     *
     * <p>Prefer calling this single method in {@code AlgoService} instead of
     * three separate calls to reduce boilerplate.</p>
     *
     * @param request fully-validated inbound DTO (must not be null)
     * @return {@link MappedRequest} containing users, tasks, and (possibly null) config
     */
    public MappedRequest toModels(SchedulingRequestDto request) {
        return new MappedRequest(
                toUserModels(request.users()),
                toTaskModels(request.tasks()),
                toModel(request.config())
        );
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Converts a {@code Set<Long>} of role IDs to a {@code Set<String>}.
     * Returns an empty set for null input.
     */
    private static Set<String> jobIdsToStrings(Set<Long> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) return Collections.emptySet();
        return jobIds.stream()
                .map(Object::toString)
                .collect(Collectors.toUnmodifiableSet());
    }

    // ── MappedRequest value object ────────────────────────────────────────────

    /**
     * Lightweight value object returned by {@link AlgoMapper#toModels(SchedulingRequestDto)}.
     * Groups users, tasks, and config into a single carry object so the service layer
     * receives everything it needs in one call.
     */
    public record MappedRequest(
            List<AlgoUser>                users,
            List<AlgoTask>                tasks,
            AlgoSchedulingConfiguration   config
    ) {}
}

