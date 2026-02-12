package com.example.mainbackend.service;

import com.example.mainbackend.dto.vacation.VacationCreateRequest;
import com.example.mainbackend.dto.vacation.VacationResponseDto;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.entity.Vacation;
import com.example.mainbackend.mapper.VacationMapper;
import com.example.mainbackend.repository.UserRepository;
import com.example.mainbackend.repository.VacationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VacationService {

    private final VacationRepository vacationRepository;
    private final UserRepository userRepository;
    private final VacationMapper mapper;

    @Transactional
    public VacationResponseDto createVacation(VacationCreateRequest request) {
        // Validate dates
        if (request.getStartDate().isAfter(request.getEndDate()))
            throw new IllegalArgumentException("Start date must be before or equal to end date");

        if (request.getStartDate().isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Start date cannot be in the past");

        // Fetch worker
        User worker = userRepository.findById(request.getWorkerId())
                .orElseThrow(() -> new IllegalArgumentException("Worker not found with ID: " + request.getWorkerId()));

        // Check for overlapping vacations
        List<Vacation> existingVacations = vacationRepository.findByWorkerId(request.getWorkerId());
        for (Vacation existing : existingVacations) {
            if (datesOverlap(existing.getStartDate(), existing.getEndDate(),
                           request.getStartDate(), request.getEndDate()))
                throw new IllegalArgumentException("Vacation period overlaps with existing vacation");
        }

        // Build and save
        Vacation vacation = Vacation.builder()
                .worker(worker)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        Vacation saved = vacationRepository.save(vacation);
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public VacationResponseDto getVacationById(Long id) {
        Vacation vacation = vacationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vacation not found with ID: " + id));
        return mapper.toDto(vacation);
    }

    @Transactional(readOnly = true)
    public List<VacationResponseDto> getAllVacations() {
        return vacationRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VacationResponseDto> getVacationsByWorker(Long workerId) {
        return vacationRepository.findByWorkerId(workerId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VacationResponseDto> getVacationsByDateRange(LocalDate startDate, LocalDate endDate) {
        return vacationRepository.findByStartDateBetween(startDate, endDate).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public VacationResponseDto updateVacation(Long id, VacationCreateRequest request) {
        Vacation existing = vacationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vacation not found with ID: " + id));

        // Validate dates
        if (request.getStartDate().isAfter(request.getEndDate()))
            throw new IllegalArgumentException("Start date must be before or equal to end date");

        // Check for overlapping vacations (excluding current one)
        List<Vacation> existingVacations = vacationRepository.findByWorkerId(request.getWorkerId());
        for (Vacation other : existingVacations) {
            if (!other.getId().equals(id) &&
                datesOverlap(other.getStartDate(), other.getEndDate(),
                           request.getStartDate(), request.getEndDate()))
                throw new IllegalArgumentException("Vacation period overlaps with existing vacation");
        }

        // Update worker if changed
        if (!existing.getWorker().getId().equals(request.getWorkerId())) {
            User worker = userRepository.findById(request.getWorkerId())
                    .orElseThrow(() -> new IllegalArgumentException("Worker not found with ID: " + request.getWorkerId()));
            existing.setWorker(worker);
        }

        // Update dates
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());

        Vacation updated = vacationRepository.save(existing);
        return mapper.toDto(updated);
    }

    @Transactional
    public boolean deleteVacation(Long id) {
        if (!vacationRepository.existsById(id))
            throw new IllegalArgumentException("Vacation not found with ID: " + id);

        vacationRepository.deleteById(id);
        return true;
    }

    /**
     * Check if two date ranges overlap.
     */
    private boolean datesOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }
}

