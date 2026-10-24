package com.spe.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test on a real embedded server. It proves that the application
 * context starts and that the Actuator health probe answers, because Docker
 * Compose uses that probe for its health check.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Application end-to-end tests")
class CalculatorApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("the actuator health endpoint reports UP")
    void healthEndpointReportsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"status\":\"UP\""),
                "Expected an UP status, but received: " + response.getBody());
    }

    @Test
    @DisplayName("the add endpoint answers on a real server")
    void addEndpointAnswersOnRealServer() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/v1/add?a=2&b=3", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"result\":5.0"),
                "Expected a result of 5.0, but received: " + response.getBody());
    }
}
