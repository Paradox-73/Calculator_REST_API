package com.spe.calculator.service;

import com.spe.calculator.exception.DivisionByZeroException;
import com.spe.calculator.exception.InvalidInputException;
import org.springframework.stereotype.Service;

/**
 * Holds the arithmetic rules of the API. The service validates the operands
 * before it calculates, and it validates the result after it calculates.
 */
@Service
public class CalculatorService {

    public double add(double a, double b) {
        validateOperands(a, b);
        return validateResult(a + b);
    }

    public double subtract(double a, double b) {
        validateOperands(a, b);
        return validateResult(a - b);
    }

    public double multiply(double a, double b) {
        validateOperands(a, b);
        return validateResult(a * b);
    }

    public double divide(double a, double b) {
        validateOperands(a, b);
        if (b == 0.0) {
            throw new DivisionByZeroException("Division by zero is not allowed. Give a non-zero value for 'b'.");
        }
        return validateResult(a / b);
    }

    private void validateOperands(double a, double b) {
        if (!Double.isFinite(a)) {
            throw new InvalidInputException("Parameter 'a' must be a finite number.");
        }
        if (!Double.isFinite(b)) {
            throw new InvalidInputException("Parameter 'b' must be a finite number.");
        }
    }

    private double validateResult(double result) {
        if (!Double.isFinite(result)) {
            throw new InvalidInputException("The result is out of the supported numeric range.");
        }
        return result;
    }
}
