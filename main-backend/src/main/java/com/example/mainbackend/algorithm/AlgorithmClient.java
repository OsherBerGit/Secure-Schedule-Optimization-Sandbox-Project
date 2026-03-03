package com.example.mainbackend.algorithm;

import com.example.mainbackend.algorithm.dto.AlgoScheduleRequest;
import com.example.mainbackend.algorithm.dto.AlgoScheduleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client that calls the algorithm side-backend.
 * Uses Spring RestClient (Spring 6 / Boot 3).
 * Target: POST http://localhost:8081/api/v1/algo/schedule
 */
@Slf4j
@Component
public class AlgorithmClient {

    private final RestClient restClient;

    public AlgorithmClient(@Value("${algorithm.service.url}") String algorithmServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(algorithmServiceUrl)
                .build();
    }

    /**
     * Sends a scheduling request to the algorithm service and returns the result.
     * @throws RuntimeException if the algorithm service is unreachable or returns an error
     */
    public AlgoScheduleResponse requestSchedule(AlgoScheduleRequest request) {
        log.info("Calling algorithm service — strategy: {}, users: {}, tasks: {}",
                request.getStrategy(),
                request.getUsers() != null ? request.getUsers().size() : 0,
                request.getTasks() != null ? request.getTasks().size() : 0);
        try {
            AlgoScheduleResponse response = restClient.post()
                    .uri("/api/v1/algo/schedule")
                    .body(request)
                    .retrieve()
                    .body(AlgoScheduleResponse.class);
            log.info("Algorithm response — assigned: {}, unassigned: {}",
                    response != null ? response.getAssignedTasks() : 0,
                    response != null ? response.getUnassignedTasks() : 0);
            return response;
        } catch (Exception e) {
            log.error("Failed to call algorithm service: {}", e.getMessage());
            throw new RuntimeException("Algorithm service unavailable: " + e.getMessage(), e);
        }
    }
}

