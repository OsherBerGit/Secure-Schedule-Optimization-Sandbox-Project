package com.example.mainbackend.service;

import com.example.mainbackend.dto.vacation.VacationCreateRequest;
import com.example.mainbackend.dto.vacation.VacationResponseDto;
import com.example.mainbackend.entity.User;
import com.example.mainbackend.entity.Vacation;
import com.example.mainbackend.mapper.VacationMapper;
import com.example.mainbackend.repository.UserRepository;
import com.example.mainbackend.repository.VacationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VacationServiceTest {

    @Mock
    private VacationRepository vacationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VacationMapper mapper;

    @InjectMocks
    private VacationService vacationService;

    private User worker;
    private Vacation vacation;
    private VacationCreateRequest request;
    private VacationResponseDto responseDto;

    @BeforeEach
    void setUp() {
        worker = User.builder()
                .id(1L)
                .nationalId("123456789")
                .firstName("John")
                .lastName("Doe")
                .build();

        vacation = Vacation.builder()
                .id(1L)
                .worker(worker)
                .startDate(LocalDate.of(2026, 3, 15))
                .endDate(LocalDate.of(2026, 3, 20))
                .build();

        request = VacationCreateRequest.builder()
                .workerId(1L)
                .startDate(LocalDate.of(2026, 3, 15))
                .endDate(LocalDate.of(2026, 3, 20))
                .build();

        responseDto = VacationResponseDto.builder()
                .id(1L)
                .workerId(1L)
                .startDate(LocalDate.of(2026, 3, 15))
                .endDate(LocalDate.of(2026, 3, 20))
                .workerName("John Doe")
                .build();
    }

    @Test
    void createVacation_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(vacationRepository.findByWorkerId(1L)).thenReturn(List.of());
        when(vacationRepository.save(any(Vacation.class))).thenReturn(vacation);
        when(mapper.toDto(vacation)).thenReturn(responseDto);

        VacationResponseDto result = vacationService.createVacation(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getWorkerId());
        verify(vacationRepository, times(1)).save(any(Vacation.class));
    }

    @Test
    void createVacation_StartDateAfterEndDate_ThrowsException() {
        request.setStartDate(LocalDate.of(2026, 3, 25));
        request.setEndDate(LocalDate.of(2026, 3, 20));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> vacationService.createVacation(request)
        );
        assertEquals("Start date must be before or equal to end date", exception.getMessage());
    }

    @Test
    void createVacation_StartDateInPast_ThrowsException() {
        request.setStartDate(LocalDate.of(2020, 1, 1));
        request.setEndDate(LocalDate.of(2020, 1, 5));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> vacationService.createVacation(request)
        );
        assertEquals("Start date cannot be in the past", exception.getMessage());
    }

    @Test
    void createVacation_WorkerNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> vacationService.createVacation(request)
        );
        assertTrue(exception.getMessage().contains("Worker not found"));
    }

    @Test
    void createVacation_OverlappingVacation_ThrowsException() {
        Vacation existingVacation = Vacation.builder()
                .id(2L)
                .worker(worker)
                .startDate(LocalDate.of(2026, 3, 10))
                .endDate(LocalDate.of(2026, 3, 18))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(vacationRepository.findByWorkerId(1L)).thenReturn(List.of(existingVacation));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> vacationService.createVacation(request)
        );
        assertEquals("Vacation period overlaps with existing vacation", exception.getMessage());
    }

    @Test
    void getVacationById_Success() {
        when(vacationRepository.findById(1L)).thenReturn(Optional.of(vacation));
        when(mapper.toDto(vacation)).thenReturn(responseDto);

        VacationResponseDto result = vacationService.getVacationById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(vacationRepository, times(1)).findById(1L);
    }

    @Test
    void getVacationById_NotFound_ThrowsException() {
        when(vacationRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> vacationService.getVacationById(999L)
        );
        assertTrue(exception.getMessage().contains("Vacation not found"));
    }

    @Test
    void getAllVacations_Success() {
        List<Vacation> vacations = List.of(vacation);
        when(vacationRepository.findAll()).thenReturn(vacations);
        when(mapper.toDto(vacation)).thenReturn(responseDto);

        List<VacationResponseDto> result = vacationService.getAllVacations();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(vacationRepository, times(1)).findAll();
    }

    @Test
    void getVacationsByWorker_Success() {
        List<Vacation> vacations = List.of(vacation);
        when(vacationRepository.findByWorkerId(1L)).thenReturn(vacations);
        when(mapper.toDto(vacation)).thenReturn(responseDto);

        List<VacationResponseDto> result = vacationService.getVacationsByWorker(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getWorkerId());
        verify(vacationRepository, times(1)).findByWorkerId(1L);
    }

    @Test
    void deleteVacation_Success() {
        when(vacationRepository.existsById(1L)).thenReturn(true);

        boolean result = vacationService.deleteVacation(1L);

        assertTrue(result);
        verify(vacationRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteVacation_NotFound_ThrowsException() {
        when(vacationRepository.existsById(999L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> vacationService.deleteVacation(999L)
        );
        assertTrue(exception.getMessage().contains("Vacation not found"));
    }
}

