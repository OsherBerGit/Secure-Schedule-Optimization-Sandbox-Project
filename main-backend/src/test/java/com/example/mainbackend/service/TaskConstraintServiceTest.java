package com.example.mainbackend.service;

import com.example.mainbackend.dto.taskconstraint.TaskConstraintCreateRequest;
import com.example.mainbackend.dto.taskconstraint.TaskConstraintResponseDto;
import com.example.mainbackend.entity.ConstraintType;
import com.example.mainbackend.entity.Task;
import com.example.mainbackend.entity.TaskConstraint;
import com.example.mainbackend.mapper.TaskConstraintMapper;
import com.example.mainbackend.repository.ConstraintTypeRepository;
import com.example.mainbackend.repository.TaskConstraintRepository;
import com.example.mainbackend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskConstraintServiceTest {

    @Mock
    private TaskConstraintRepository taskConstraintRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ConstraintTypeRepository constraintTypeRepository;

    @Mock
    private TaskConstraintMapper mapper;

    @InjectMocks
    private TaskConstraintService taskConstraintService;

    private Task task1;
    private Task task2;
    private Task task3;
    private ConstraintType constraintType;
    private TaskConstraint constraint;
    private TaskConstraintCreateRequest request;
    private TaskConstraintResponseDto responseDto;

    @BeforeEach
    void setUp() {
        // Setup test data
        task1 = Task.builder()
                .id(1L)
                .title("Task 1")
                .build();

        task2 = Task.builder()
                .id(2L)
                .title("Task 2")
                .build();

        task3 = Task.builder()
                .id(3L)
                .title("Task 3")
                .build();

        constraintType = ConstraintType.builder()
                .id(1L)
                .name("FINISH_TO_START")
                .description("Successor cannot start until predecessor finishes")
                .build();

        constraint = TaskConstraint.builder()
                .id(1L)
                .predecessorTask(task1)
                .successorTask(task2)
                .constraintType(constraintType)
                .lagMinutes(0)
                .build();

        request = TaskConstraintCreateRequest.builder()
                .predecessorTaskId(1L)
                .successorTaskId(2L)
                .constraintTypeId(1L)
                .lagMinutes(0)
                .build();

        responseDto = TaskConstraintResponseDto.builder()
                .id(1L)
                .predecessorTaskId(1L)
                .successorTaskId(2L)
                .constraintTypeId(1L)
                .lagMinutes(0)
                .predecessorTaskTitle("Task 1")
                .successorTaskTitle("Task 2")
                .constraintTypeName("FINISH_TO_START")
                .build();
    }

    @Test
    void createConstraint_Success() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task1));
        when(taskRepository.findById(2L)).thenReturn(Optional.of(task2));
        when(constraintTypeRepository.findById(1L)).thenReturn(Optional.of(constraintType));
        when(taskConstraintRepository.existsByPredecessorTaskIdAndSuccessorTaskId(1L, 2L)).thenReturn(false);
        when(taskConstraintRepository.findAll()).thenReturn(new ArrayList<>());
        when(taskConstraintRepository.save(any(TaskConstraint.class))).thenReturn(constraint);
        when(mapper.toDto(constraint)).thenReturn(responseDto);

        // Act
        TaskConstraintResponseDto result = taskConstraintService.createConstraint(request);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getPredecessorTaskId());
        assertEquals(2L, result.getSuccessorTaskId());
        verify(taskConstraintRepository, times(1)).save(any(TaskConstraint.class));
    }

    @Test
    void createConstraint_SelfReference_ThrowsException() {
        // Arrange
        request.setSuccessorTaskId(1L); // Same as predecessor

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskConstraintService.createConstraint(request)
        );
        assertEquals("A task cannot have a constraint with itself", exception.getMessage());
    }

    @Test
    void createConstraint_DuplicateConstraint_ThrowsException() {
        // Arrange
        when(taskConstraintRepository.existsByPredecessorTaskIdAndSuccessorTaskId(1L, 2L)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskConstraintService.createConstraint(request)
        );
        assertEquals("Constraint already exists between these tasks", exception.getMessage());
    }

    @Test
    void createConstraint_PredecessorNotFound_ThrowsException() {
        // Arrange
        when(taskConstraintRepository.existsByPredecessorTaskIdAndSuccessorTaskId(1L, 2L)).thenReturn(false);
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskConstraintService.createConstraint(request)
        );
        assertTrue(exception.getMessage().contains("Predecessor task not found"));
    }

    @Test
    void createConstraint_CircularDependency_ThrowsException() {
        // Arrange: Create constraint 1->2, then try to add 2->1
        TaskConstraint existingConstraint = TaskConstraint.builder()
                .predecessorTask(task1)
                .successorTask(task2)
                .build();

        // Request to create 2->1 (would create cycle)
        TaskConstraintCreateRequest circularRequest = TaskConstraintCreateRequest.builder()
                .predecessorTaskId(2L)
                .successorTaskId(1L)
                .constraintTypeId(1L)
                .lagMinutes(0)
                .build();

        when(taskRepository.findById(2L)).thenReturn(Optional.of(task2));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task1));
        when(constraintTypeRepository.findById(1L)).thenReturn(Optional.of(constraintType));
        when(taskConstraintRepository.existsByPredecessorTaskIdAndSuccessorTaskId(2L, 1L)).thenReturn(false);
        when(taskConstraintRepository.findAll()).thenReturn(List.of(existingConstraint));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskConstraintService.createConstraint(circularRequest)
        );
        assertTrue(exception.getMessage().contains("circular dependency"));
    }

    @Test
    void getConstraintById_Success() {
        // Arrange
        when(taskConstraintRepository.findById(1L)).thenReturn(Optional.of(constraint));
        when(mapper.toDto(constraint)).thenReturn(responseDto);

        // Act
        TaskConstraintResponseDto result = taskConstraintService.getConstraintById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(taskConstraintRepository, times(1)).findById(1L);
    }

    @Test
    void getConstraintById_NotFound_ThrowsException() {
        // Arrange
        when(taskConstraintRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskConstraintService.getConstraintById(999L)
        );
        assertTrue(exception.getMessage().contains("Task constraint not found"));
    }

    @Test
    void getAllConstraints_Success() {
        // Arrange
        List<TaskConstraint> constraints = List.of(constraint);
        when(taskConstraintRepository.findAll()).thenReturn(constraints);
        when(mapper.toDto(constraint)).thenReturn(responseDto);

        // Act
        List<TaskConstraintResponseDto> result = taskConstraintService.getAllConstraints();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskConstraintRepository, times(1)).findAll();
    }

    @Test
    void getConstraintsByPredecessorTask_Success() {
        // Arrange
        List<TaskConstraint> constraints = List.of(constraint);
        when(taskConstraintRepository.findByPredecessorTaskId(1L)).thenReturn(constraints);
        when(mapper.toDto(constraint)).thenReturn(responseDto);

        // Act
        List<TaskConstraintResponseDto> result = taskConstraintService.getConstraintsByPredecessorTask(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getPredecessorTaskId());
        verify(taskConstraintRepository, times(1)).findByPredecessorTaskId(1L);
    }

    @Test
    void getConstraintsBySuccessorTask_Success() {
        // Arrange
        List<TaskConstraint> constraints = List.of(constraint);
        when(taskConstraintRepository.findBySuccessorTaskId(2L)).thenReturn(constraints);
        when(mapper.toDto(constraint)).thenReturn(responseDto);

        // Act
        List<TaskConstraintResponseDto> result = taskConstraintService.getConstraintsBySuccessorTask(2L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getSuccessorTaskId());
        verify(taskConstraintRepository, times(1)).findBySuccessorTaskId(2L);
    }

    @Test
    void deleteConstraint_Success() {
        // Arrange
        when(taskConstraintRepository.existsById(1L)).thenReturn(true);

        // Act
        boolean result = taskConstraintService.deleteConstraint(1L);

        // Assert
        assertTrue(result);
        verify(taskConstraintRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteConstraint_NotFound_ThrowsException() {
        // Arrange
        when(taskConstraintRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskConstraintService.deleteConstraint(999L)
        );
        assertTrue(exception.getMessage().contains("Task constraint not found"));
    }
}

