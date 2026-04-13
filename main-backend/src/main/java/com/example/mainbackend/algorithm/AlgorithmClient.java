package com.example.mainbackend.algorithm;

import com.example.mainbackend.algorithm.dto.AlgoScheduleRequest;
import com.example.mainbackend.algorithm.dto.AlgoScheduleResponse;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP client that calls the algorithm side-backend.
 * Uses Spring RestClient (Spring 6 / Boot 3).
 * Target: POST http://localhost:8081/api/v1/algo/schedule
 */
@Slf4j
@Component
public class AlgorithmClient {

    private final RestClient restClient;
    private final Validator validator;

    public AlgorithmClient(@Value("${algorithm.service.url}") String algorithmServiceUrl, Validator validator) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(java.time.Duration.ofMinutes(2));

        this.restClient = RestClient.builder()
                .baseUrl(algorithmServiceUrl)
                .build();
        this.validator = validator;
    }

    public AlgoScheduleResponse requestSchedule(AlgoScheduleRequest request) {
        log.info("Requesting schedule from Algorithm Service [Strategy: {}]", request.getStrategy());

        try {
            AlgoScheduleResponse response = restClient.post()
                    .uri("/api/v1/algo/schedule")
                    .body(request)
                    .retrieve()
                    .body(AlgoScheduleResponse.class);

            if (response == null)
                throw new RuntimeException("Algorithm service returned empty body");

            var violations = validator.validate(response);
            if (!violations.isEmpty()) {
                log.error("Invalid response from Algorithm: {}", violations);
                throw new RuntimeException("Algorithm response failed validation");
            }

            log.info("Algorithm calculation successful: {} tasks assigned, {} remains unassigned.",
                    response.getAssignedTasks(), response.getUnassignedTasks());

            return response;
        } catch (RestClientResponseException e) {
            log.error("Algorithm service returned HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Algorithm calculation failed: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to connect to algorithm service: {}", e.getMessage());
            throw new RuntimeException("Algorithm service unreachable: " + e.getMessage(), e);
        }
    }
}

