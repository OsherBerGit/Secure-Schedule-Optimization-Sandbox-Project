package com.example.mainbackend.controller;

import com.example.mainbackend.dto.task.TaskCreateRequest;
import com.example.mainbackend.dto.task.TaskResponseDto;
import com.example.mainbackend.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "http://localhost:5173") // Vite dev server
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponseDto> createTask(@RequestBody TaskCreateRequest request) {
        TaskResponseDto createdTask = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    /**
     * Retrieves all tasks.
     *
     * @return ResponseEntity with list of all tasks and HTTP 200 status
     */
    @GetMapping
    public ResponseEntity<List<TaskResponseDto>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDto> getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDto> updateTask(@PathVariable Long id, @RequestBody TaskCreateRequest request) {
        return taskService.updateTask(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        if (taskService.deleteTask(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<TaskResponseDto>> getTasksByWorker(@PathVariable Long workerId) {
        return ResponseEntity.ok(taskService.getTasksByWorkerId(workerId));
    }

    /**
     * Retrieves all tasks with a specific status.
     *
     * @param statusName the status name to filter by
     * @return ResponseEntity with list of tasks with the specified status and HTTP 200 status
     */
    @GetMapping("/status/{statusName}")
    public ResponseEntity<List<TaskResponseDto>> getTasksByStatus(@PathVariable String statusName) {
        return ResponseEntity.ok(taskService.getTasksByStatus(statusName));
    }
}
