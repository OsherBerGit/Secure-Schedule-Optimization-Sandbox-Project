package com.example.mainbackend.algorithm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MemeticScheduleResponse extends AlgoScheduleResponse {
    private List<Double> fitnessHistory;
}
