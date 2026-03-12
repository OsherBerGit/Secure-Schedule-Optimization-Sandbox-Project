package com.example.mainbackend.service;

import com.example.mainbackend.entity.Department;
import com.example.mainbackend.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<Department> getAll() {
        return departmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Department> getById(Long id) {
        return departmentRepository.findById(id);
    }

    @Transactional
    public Department create(String name) {
        if (departmentRepository.findByName(name).isPresent())
            throw new IllegalArgumentException("Department already exists: " + name);
        return departmentRepository.save(Department.builder().name(name).build());
    }

    @Transactional
    public Optional<Department> update(Long id, String name) {
        return departmentRepository.findById(id).map(dept -> {
            dept.setName(name);
            return departmentRepository.save(dept);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (departmentRepository.existsById(id)) {
            departmentRepository.deleteById(id);
            return true;
        }
        return false;
    }
}

