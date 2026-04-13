package com.example.sidebackend.service;

import com.example.algorithm.model.*;
import com.example.sidebackend.dto.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public final class AlgoMapper {

    public AlgoVacation toModel(VacationDto dto, Long userId) {
        return new AlgoVacation(
                dto.id(),
                userId,
                dto.startDate(),
                dto.endDate()
        );
    }

    public List<AlgoVacation> toVacationModels(List<VacationDto> dtos, Long userId) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        List<AlgoVacation> result = new ArrayList<>(dtos.size());
        for (VacationDto dto : dtos)
            result.add(toModel(dto, userId));
        return result;
    }

    public AlgoWorkerAvailability toModel(UserDto.WorkerAvailabilityDto dto) {
        return new AlgoWorkerAvailability(dto.dayOfWeek(), dto.startTime(), dto.endTime());
    }

    public List<AlgoWorkerAvailability> toAvailabilityModels(List<UserDto.WorkerAvailabilityDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        List<AlgoWorkerAvailability> result = new ArrayList<>(dtos.size());
        for (UserDto.WorkerAvailabilityDto dto : dtos)
            result.add(toModel(dto));
        return result;
    }

    public AlgoUser toModel(UserDto dto) {
        Set<Long> skills = dto.skillIds() != null ? Set.copyOf(dto.skillIds()) : Collections.emptySet();
        List<AlgoVacation> vacations = toVacationModels(dto.vacations(), dto.id());
        List<AlgoWorkerAvailability> availabilities = toAvailabilityModels(dto.availabilities());
        return new AlgoUser(dto.id(), availabilities, dto.maxTasks(), skills, vacations);
    }

    public List<AlgoUser> toUserModels(List<UserDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        List<AlgoUser> result = new ArrayList<>(dtos.size());
        for (UserDto dto : dtos)
            result.add(toModel(dto));
        return result;
    }

    public AlgoTask toModel(TaskDto dto) {
        Set<Long> requiredSkills = (dto.requiredSkillIds() != null)
                ? new java.util.HashSet<>(dto.requiredSkillIds())
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
                requiredSkills,
                constraints
        );
    }

    public List<AlgoTask> toTaskModels(List<TaskDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
        List<AlgoTask> result = new ArrayList<>(dtos.size());
        for (TaskDto dto : dtos)
            result.add(toModel(dto));
        return result;
    }

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

    public MappedRequest toModels(SchedulingRequestDto request) {
        return new MappedRequest(
                toUserModels(request.users()),
                toTaskModels(request.tasks()),
                toModel(request.config())
        );
    }

    public record MappedRequest(
            List<AlgoUser>                users,
            List<AlgoTask>                tasks,
            AlgoSchedulingConfiguration   config
    ) {}
}

