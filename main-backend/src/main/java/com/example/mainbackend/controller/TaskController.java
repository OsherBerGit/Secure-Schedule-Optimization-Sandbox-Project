package com.example.mainbackend.controller;

import com.example.mainbackend.dto.task.TaskCreateRequest;
import com.example.mainbackend.dto.task.TaskResponseDto;
import com.example.mainbackend.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @securityHelper.canManageDepartment(#request.departmentId))")
    public ResponseEntity<TaskResponseDto> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskResponseDto createdTask = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskResponseDto>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@securityHelper.canViewTask(#id)")
    public ResponseEntity<TaskResponseDto> getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityHelper.canManageTask(#id)")
    public ResponseEntity<TaskResponseDto> updateTask(@PathVariable Long id, @Valid @RequestBody TaskCreateRequest request) {
        return taskService.updateTask(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@securityHelper.canManageTask(#id)")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        if (taskService.deleteTask(id))
            return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/worker/{workerId}")
    @PreAuthorize("@securityHelper.canManageUser(#workerId)")
    public ResponseEntity<List<TaskResponseDto>> getTasksByWorker(@PathVariable Long workerId) {
        return ResponseEntity.ok(taskService.getTasksByWorkerId(workerId));
    }

    @GetMapping("/{id}/valid-prerequisites")
    @PreAuthorize("@securityHelper.canManageTask(#id)")
    public ResponseEntity<List<TaskResponseDto>> getValidPrerequisites(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getValidPrerequisites(id));
    }

    @GetMapping("/status/{statusId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TaskResponseDto>> getTasksByStatus(@PathVariable Long statusId) {
        return ResponseEntity.ok(taskService.getTasksByStatusId(statusId));
    }
}
