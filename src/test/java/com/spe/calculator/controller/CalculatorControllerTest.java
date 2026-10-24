package com.spe.calculator.controller;

import com.spe.calculator.exception.GlobalExceptionHandler;
import com.spe.calculator.service.CalculatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring MVC integration tests. They send real HTTP requests through the
 * dispatcher servlet to the controller, the service, and the error handler.
 */
@WebMvcTest(CalculatorController.class)
@Import({CalculatorService.class, GlobalExceptionHandler.class})
@DisplayName("Calculator API web layer tests")
class CalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "GET /api/v1/{0}?a={1}&b={2} returns {3}")
    @CsvSource({
            "add, 2, 3, 5.0",
            "subtract, 5, 3, 2.0",
            "multiply, 4, 2.5, 10.0",
            "divide, 7, 2, 3.5"
    })
    void returnsResultForEachOperation(String operation, String a, String b, double expected) throws Exception {
        mockMvc.perform(get("/api/v1/{operation}", operation).param("a", a).param("b", b))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value(operation))
                .andExpect(jsonPath("$.a").value(Double.parseDouble(a)))
                .andExpect(jsonPath("$.b").value(Double.parseDouble(b)))
                .andExpect(jsonPath("$.result").value(expected));
    }

    @Test
    @DisplayName("divide by zero returns HTTP 400 and a structured error body")
    void divideByZeroReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/divide").param("a", "10").param("b", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Division by zero"))
                .andExpect(jsonPath("$.path").value("/api/v1/divide"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("a missing parameter returns HTTP 400")
    void missingParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Missing parameter"))
                .andExpect(jsonPath("$.message").value("Required parameter 'b' is missing."));
    }

    @Test
    @DisplayName("a non-numeric parameter returns HTTP 400")
    void nonNumericParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "abc").param("b", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid parameter"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
