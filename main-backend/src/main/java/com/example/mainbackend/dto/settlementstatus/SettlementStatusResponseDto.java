package com.example.mainbackend.dto.settlementstatus;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Builder
public class SettlementStatusResponseDto {
    private Long id;
    private String name;
}
