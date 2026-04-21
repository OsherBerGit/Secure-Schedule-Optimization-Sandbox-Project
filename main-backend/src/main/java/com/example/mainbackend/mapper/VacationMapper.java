package com.example.mainbackend.mapper;

import com.example.mainbackend.dto.vacation.VacationResponseDto;
import com.example.mainbackend.entity.Vacation;
import org.springframework.stereotype.Component;

@Component
public class VacationMapper {

    public VacationResponseDto toDto(Vacation vacation) {
        if (vacation == null) return null;

        return VacationResponseDto.builder()
                .id(vacation.getId())
                .userId(vacation.getUser().getId())
                .startDate(vacation.getStartDate())
                .endDate(vacation.getEndDate())
                .userName(vacation.getUser().getFirstName() + " " + vacation.getUser().getLastName())
                .statusName(vacation.getStatus() != null ? vacation.getStatus().getName() : null)
                .build();
    }
}
