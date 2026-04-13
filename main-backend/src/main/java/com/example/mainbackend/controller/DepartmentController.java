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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityHelper.getCurrentUserDepartmentId() == #id")
    public ResponseEntity<Department> getDepartmentById(@PathVariable Long id) {
        return departmentService.getDepartmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> createDepartment(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank())
            return ResponseEntity.badRequest().build();
        Department created = departmentService.createDepartment(name.trim());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Department> updateDepartment(@PathVariable Long id,
                                             @RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank())
            return ResponseEntity.badRequest().build();
        return departmentService.updateDepartment(id, name.trim())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (departmentService.deleteDepartment(id))
            return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }
}

