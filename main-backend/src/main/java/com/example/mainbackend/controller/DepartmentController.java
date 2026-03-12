package com.example.mainbackend.controller;

import com.example.mainbackend.entity.Department;
import com.example.mainbackend.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Department>> getAll() {
        return ResponseEntity.ok(departmentService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> getById(@PathVariable Long id) {
        return departmentService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank())
            return ResponseEntity.badRequest().build();
        Department created = departmentService.create(name.trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> update(@PathVariable Long id,
                                             @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank())
            return ResponseEntity.badRequest().build();
        return departmentService.update(id, name.trim())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (departmentService.delete(id))
            return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }
}

