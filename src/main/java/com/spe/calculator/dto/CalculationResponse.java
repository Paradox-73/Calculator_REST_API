package com.spe.calculator.dto;

/**
 * Successful response body, for example:
 * { "operation": "add", "a": 2.0, "b": 3.0, "result": 5.0 }
 */
public record CalculationResponse(String operation, double a, double b, double result) {
}
