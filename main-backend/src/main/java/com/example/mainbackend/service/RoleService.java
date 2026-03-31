package com.example.mainbackend.service;

import com.example.mainbackend.dto.role.RoleResponseDto;
import com.example.mainbackend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public List<RoleResponseDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> RoleResponseDto.builder()
                        .name(role.getRoleName())
                        .build())
                .toList();
    }
}
