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

    /**
     * Creates a new task.
     * ALLOWED FOR: ADMIN, or MANAGER (if they are creating it for their own department).
     * Note: Assuming TaskCreateRequest has a getDepartmentId() method.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @securityHelper.canManageDepartment(#request.departmentId))")
    public ResponseEntity<TaskResponseDto> createTask(@Valid @RequestBody TaskCreateRequest request) {
        TaskResponseDto createdTask = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    /**
     * Retrieves all tasks.
     * RESTRICTED TO ADMIN ONLY. (Managers/Workers should fetch by department/user).
     * @return ResponseEntity with list of all tasks and HTTP 200 status
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<List<TaskResponseDto>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    /**
     * Retrieves a specific task by its ID.
     * ALLOWED FOR: ADMIN, MANAGER of the task's department, or WORKER assigned to this task.
     */
    @GetMapping("/{id}")
    @PreAuthorize("@securityHelper.canViewTask(#id)")
    public ResponseEntity<TaskResponseDto> getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Updates an existing task.
     * ALLOWED FOR: ADMIN, or MANAGER of the task's department.
     * Workers CANNOT edit tasks (they only report completion, maybe via a different endpoint).
     */
    @PutMapping("/{id}")
    @PreAuthorize("@securityHelper.canManageTask(#id)")
    public ResponseEntity<TaskResponseDto> updateTask(@PathVariable Long id, @Valid @RequestBody TaskCreateRequest request) {
        return taskService.updateTask(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deletes a task by its ID.
     * ALLOWED FOR: ADMIN, or MANAGER of the task's department.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@securityHelper.canManageTask(#id)")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        if (taskService.deleteTask(id))
            return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    /**
     * Retrieves tasks assigned to a specific worker.
     * ALLOWED FOR: ADMIN, MANAGER (if worker is in their dept), or the WORKER themselves.
     */
    @GetMapping("/worker/{workerId}")
    @PreAuthorize("@securityHelper.canManageUser(#workerId)")
    public ResponseEntity<List<TaskResponseDto>> getTasksByWorker(@PathVariable Long workerId) {
        return ResponseEntity.ok(taskService.getTasksByWorkerId(workerId));
    }

    /**
     * Retrieves all tasks with a specific status.
     * RESTRICTED TO ADMIN ONLY (Global search).
     */
    @GetMapping("/status/{statusId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TaskResponseDto>> getTasksByStatus(@PathVariable Long statusId) {
        return ResponseEntity.ok(taskService.getTasksByStatusId(statusId));
    }
}
