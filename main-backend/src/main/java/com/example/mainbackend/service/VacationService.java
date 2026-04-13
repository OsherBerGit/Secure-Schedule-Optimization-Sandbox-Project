package com.example.mainbackend.service;

import com.example.mainbackend.constants.VacationStatusLevel;
import com.example.mainbackend.dto.vacation.VacationCreateRequest;
import com.example.mainbackend.dto.vacation.VacationRequestDto;
import com.example.mainbackend.dto.vacation.VacationResponseDto;
import com.example.mainbackend.dto.vacation.VacationStatusUpdateRequest;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.entity.Vacation;
import com.example.mainbackend.entity.VacationStatus;
import com.example.mainbackend.mapper.VacationMapper;
import com.example.mainbackend.repository.UserRepository;
import com.example.mainbackend.repository.VacationRepository;
import com.example.mainbackend.repository.VacationStatusRepository;
import com.example.mainbackend.security.SecurityHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VacationService {

    private final VacationRepository vacationRepository;
    private final UserRepository userRepository;
    private final VacationStatusRepository vacationStatusRepository;
    private final VacationMapper mapper;
    private final SecurityHelper securityHelper;

    @Transactional
    public VacationResponseDto createVacation(VacationCreateRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate()))
            throw new IllegalArgumentException("Start date must be before or equal to end date");

        if (request.getStartDate().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Start date cannot be in the past");

        User worker = userRepository.findById(request.getWorkerId())
                .orElseThrow(() -> new IllegalArgumentException("Worker not found with ID: " + request.getWorkerId()));

        List<Vacation> existing = vacationRepository.findByWorkerId(request.getWorkerId());
        for (Vacation v : existing)
            if (datesOverlap(v.getStartDate(), v.getEndDate(), request.getStartDate(), request.getEndDate()))
                throw new IllegalArgumentException("Vacation period overlaps with an existing vacation");

        VacationStatus approved = vacationStatusRepository.findByName(VacationStatusLevel.APPROVED.name())
                .orElseThrow(() -> new IllegalStateException("APPROVED vacation status not found in database"));

        Vacation vacation = Vacation.builder()
                .worker(worker)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(approved)
                .build();

        return mapper.toDto(vacationRepository.save(vacation));
    }

    @Transactional
    public VacationResponseDto requestVacation(VacationRequestDto request) {
        if (request.getStartDate().isAfter(request.getEndDate()))
            throw new IllegalArgumentException("Start date must be before or equal to end date");

        User worker = userRepository.findById(request.getWorkerId())
                .orElseThrow(() -> new IllegalArgumentException("Worker not found with ID: " + request.getWorkerId()));

        List<Vacation> existing = vacationRepository.findByWorkerId(worker.getId());
        for (Vacation v : existing)
            if (datesOverlap(v.getStartDate(), v.getEndDate(), request.getStartDate(), request.getEndDate()))
                throw new IllegalArgumentException("Vacation period overlaps with an existing vacation");

        VacationStatus pending = vacationStatusRepository.findByName(VacationStatusLevel.PENDING.name())
                .orElseThrow(() -> new IllegalStateException("PENDING vacation status not found in database"));

        Vacation vacation = Vacation.builder()
                .worker(worker)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(pending)
                .build();

        return mapper.toDto(vacationRepository.save(vacation));
    }

    @Transactional
    public VacationResponseDto updateVacationStatus(Long id, VacationStatusUpdateRequest request) {
        Vacation vacation = vacationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vacation not found with ID: " + id));

        // RBAC Check for MANAGER
        if (securityHelper.isManager()) {
            Long deptId = securityHelper.getCurrentUserDepartmentId();
            if (vacation.getWorker().getDepartment() == null || !vacation.getWorker().getDepartment().getId().equals(deptId))
                throw new AccessDeniedException("Managers can only manage vacations for their own department.");
        }

        if (vacation.getStatus() == null || !VacationStatusLevel.PENDING.name().equals(vacation.getStatus().getName()))
            throw new IllegalStateException("Only PENDING vacation requests can be approved or rejected");

        VacationStatus newStatus = vacationStatusRepository.findByName(request.getStatus())
                .orElseThrow(() -> new IllegalArgumentException("Vacation status not found: " + request.getStatus()));

        vacation.setStatus(newStatus);
        return mapper.toDto(vacationRepository.save(vacation));
    }

    @Transactional(readOnly = true)
    public List<VacationResponseDto> getPendingVacations() {
        if (securityHelper.isManager()) {
            Long deptId = securityHelper.getCurrentUserDepartmentId();
            return vacationRepository.findAllByWorker_Department_IdAndStatus_Name(deptId, VacationStatusLevel.PENDING.name()).stream()
                    .map(mapper::toDto)
                    .toList();
        }
        return vacationRepository.findByStatus_Name(VacationStatusLevel.PENDING.name()).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public VacationResponseDto getVacationById(Long id) {
        return mapper.toDto(
                vacationRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Vacation not found with ID: " + id))
        );
    }

    @Transactional(readOnly = true)
    public List<VacationResponseDto> getAllVacations() {
        if (securityHelper.isManager()) {
            Long deptId = securityHelper.getCurrentUserDepartmentId();
            return vacationRepository.findAllByWorker_Department_Id(deptId).stream()
                    .map(mapper::toDto)
                    .toList();
        }
        return vacationRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<VacationResponseDto> getVacationsByWorker(Long workerId) {
        return vacationRepository.findByWorkerId(workerId).stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<VacationResponseDto> getVacationsByDateRange(LocalDate startDate, LocalDate endDate) {
        return vacationRepository.findByStartDateBetween(startDate, endDate).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public VacationResponseDto updateVacation(Long id, VacationCreateRequest request) {
        Vacation existing = vacationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vacation not found with ID: " + id));

        if (request.getStartDate().isAfter(request.getEndDate()))
            throw new IllegalArgumentException("Start date must be before or equal to end date");

        List<Vacation> others = vacationRepository.findByWorkerId(request.getWorkerId());
        for (Vacation other : others) {
            if (!other.getId().equals(id) &&
                    datesOverlap(other.getStartDate(), other.getEndDate(), request.getStartDate(), request.getEndDate()))
                throw new IllegalArgumentException("Vacation period overlaps with an existing vacation");
        }

        if (!existing.getWorker().getId().equals(request.getWorkerId())) {
            User worker = userRepository.findById(request.getWorkerId())
                    .orElseThrow(() -> new IllegalArgumentException("Worker not found with ID: " + request.getWorkerId()));
            existing.setWorker(worker);
        }

        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());

        return mapper.toDto(vacationRepository.save(existing));
    }

    @Transactional
    public boolean deleteVacation(Long id) {
        if (!vacationRepository.existsById(id))
            throw new IllegalArgumentException("Vacation not found with ID: " + id);
        vacationRepository.deleteById(id);
        return true;
    }

    private boolean datesOverlap(LocalDate s1, LocalDate e1, LocalDate s2, LocalDate e2) {
        return !s1.isAfter(e2) && !s2.isAfter(e1);
    }
}
