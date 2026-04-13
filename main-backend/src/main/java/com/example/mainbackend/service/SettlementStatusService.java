package com.example.mainbackend.service;

import com.example.mainbackend.dto.settlementstatus.SettlementStatusResponseDto;
import com.example.mainbackend.mapper.SettlementStatusMapper;
import com.example.mainbackend.repository.SettlementStatusRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementStatusService {

    private final SettlementStatusRepository settlementStatusRepository;
    private final SettlementStatusMapper mapper;

    @Transactional(readOnly = true)
    public List<SettlementStatusResponseDto> getAllStatuses() {
        return settlementStatusRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SettlementStatusResponseDto getStatusById(Long id) {
        return settlementStatusRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Settlement status not found with id: " + id));
    }
}
