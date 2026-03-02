package com.example.mainbackend.mapper;

import com.example.mainbackend.dto.vacation.VacationResponseDto;
import com.example.mainbackend.entity.Vacation;
import org.springframework.stereotype.Component;

@Component
public class VacationMapper {

    public VacationResponseDto toDto(Vacation vacation) {
        if (vacation == null)
            return null;

        return VacationResponseDto.builder()
                .id(vacation.getId())
                .workerId(vacation.getWorker().getId())
                .startDate(vacation.getStartDate())
                .endDate(vacation.getEndDate())
                .workerName(vacation.getWorker().getFirstName() + " " + vacation.getWorker().getLastName())
                .statusName(vacation.getStatus() != null ? vacation.getStatus().getName() : null)
                .build();
    }
}
