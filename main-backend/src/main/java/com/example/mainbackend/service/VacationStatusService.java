package com.example.mainbackend.service;

import com.example.mainbackend.dto.vacationstatus.VacationStatusResponseDto;
import com.example.mainbackend.mapper.VacationStatusMapper;
import com.example.mainbackend.repository.VacationStatusRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VacationStatusService {

    private final VacationStatusRepository repository;
    private final VacationStatusMapper mapper;

    @Transactional(readOnly = true)
    public List<VacationStatusResponseDto> getAllStatuses() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VacationStatusResponseDto getStatusById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Vacation status not found"));
    }
}
