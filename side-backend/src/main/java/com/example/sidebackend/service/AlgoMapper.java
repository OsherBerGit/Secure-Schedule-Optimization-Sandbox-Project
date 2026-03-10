package com.example.sidebackend.service;

import com.example.algorithm.model.AlgoSchedulingConfiguration;
import com.example.algorithm.model.AlgoTask;
import com.example.algorithm.model.AlgoUser;
import com.example.algorithm.model.AlgoVacation;
import com.example.sidebackend.dto.SchedulingConfigurationDto;
import com.example.sidebackend.dto.SchedulingRequestDto;
import com.example.sidebackend.dto.TaskDto;
import com.example.sidebackend.dto.UserDto;
import com.example.sidebackend.dto.VacationDto;

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
 *   <li>No PII fields (names, emails) are ever copied — the DTOs don't carry them
 *       and the models don't accept them.</li>
 *   <li>Every {@code null} collection is normalised to an empty, immutable collection
 *       so the algorithm engine never needs to perform null checks on lists/sets.</li>
 * </ul>
 *
 * <h3>Role ID convention</h3>
 * Both {@code UserDto.roleIds} and {@code TaskDto.requiredRoleIds} are {@code Set<Long>}.
 * {@code AlgoUser.roles} and {@code AlgoTask.requiredRoles} are {@code Set<String>}.
 * The mapper converts each {@code Long} to its {@code String} representation (e.g. {@code 42L → "42"})
 * so that the engine's role-matching logic can do a simple {@code Set.contains()} check.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 *   AlgoUser  user   = AlgoMapper.toModel(userDto);
 *   AlgoTask  task   = AlgoMapper.toModel(taskDto);
 *   AlgoUser  user   = AlgoMapper.toModel(userDto, vacationDtos);  // explicit vacation list
 *   // Full request mapping:
 *   List<AlgoUser> users = AlgoMapper.toUserModels(request.users());
 *   List<AlgoTask> tasks = AlgoMapper.toTaskModels(request.tasks());
 *   AlgoSchedulingConfiguration cfg = AlgoMapper.toModel(request.config());
 * }</pre>
 */
public final class AlgoMapper {

    // Non-instantiable utility class
    private AlgoMapper() {}

    // ── VacationDto → AlgoVacation ────────────────────────────────────────────

    /**
     * Converts a single {@link VacationDto} belonging to the given worker.
     *
     * @param dto    source vacation record (must not be null)
     * @param userId the ID of the owning worker (for traceability inside the engine)
     * @return immutable {@link AlgoVacation}
     */
    public static AlgoVacation toModel(VacationDto dto, Long userId) {
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
    public static List<AlgoVacation> toVacationModels(List<VacationDto> dtos, Long userId) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        List<AlgoVacation> result = new ArrayList<>(dtos.size());
        for (VacationDto dto : dtos)
            result.add(toModel(dto, userId));
        return result;
    }

    // ── UserDto → AlgoUser ────────────────────────────────────────────────────

    /**
     * Converts a single {@link UserDto} to an {@link AlgoUser} model.
     * Vacations are read directly from {@code dto.vacations()}.
     *
     * @param dto source DTO (must not be null)
     * @return immutable {@link AlgoUser}
     */
    public static AlgoUser toModel(UserDto dto) {
        Set<String> roles = roleIdsToStrings(dto.roleIds());
        List<AlgoVacation> vacations = toVacationModels(dto.vacations(), dto.id());
        return new AlgoUser(dto.id(), dto.dailyAvailabilityHours(), dto.maxTasks(), roles, vacations);
    }

    /**
     * Bulk-converts a list of {@link UserDto}s.
     * Returns an empty list when the input is null or empty.
     */
    public static List<AlgoUser> toUserModels(List<UserDto> dtos) {
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
    public static AlgoTask toModel(TaskDto dto) {
        Set<String> requiredRoles = roleIdsToStrings(dto.requiredRoleIds());
        List<Long> predecessors   = dto.predecessorTaskIds() != null
                ? Collections.unmodifiableList(new ArrayList<>(dto.predecessorTaskIds()))
                : Collections.emptyList();
        return new AlgoTask(
                dto.id(),
                dto.durationHours(),
                dto.deadline(),
                dto.priorityLevel(),
                requiredRoles,
                predecessors
        );
    }

    /**
     * Bulk-converts a list of {@link TaskDto}s.
     * Returns an empty list when the input is null or empty.
     */
    public static List<AlgoTask> toTaskModels(List<TaskDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        List<AlgoTask> result = new ArrayList<>(dtos.size());
        for (TaskDto dto : dtos) {
            result.add(toModel(dto));
        }
        return result;
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
    public static AlgoSchedulingConfiguration toModel(SchedulingConfigurationDto dto) {
        if (dto == null) return null;
        return new AlgoSchedulingConfiguration(
                dto.weightPriority(),
                dto.weightDeadline(),
                dto.weightFairness(),
                dto.populationSize(),
                dto.maxGenerations()
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
    public static MappedRequest toModels(SchedulingRequestDto request) {
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
    private static Set<String> roleIdsToStrings(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return Collections.emptySet();
        return roleIds.stream()
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

