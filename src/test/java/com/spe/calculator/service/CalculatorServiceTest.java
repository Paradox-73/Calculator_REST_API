package com.spe.calculator.service;

import com.spe.calculator.exception.DivisionByZeroException;
import com.spe.calculator.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("CalculatorService unit tests")
class CalculatorServiceTest {

    private static final double DELTA = 1e-9;

    private CalculatorService service;

    @BeforeEach
    void setUp() {
        service = new CalculatorService();
    }

    @Nested
    @DisplayName("add")
    class Add {

        @ParameterizedTest(name = "{0} + {1} = {2}")
        @CsvSource({
                "2, 3, 5",
                "-2, 3, 1",
                "-2, -3, -5",
                "0, 0, 0",
                "2.5, 0.25, 2.75"
        })
        void addsTwoNumbers(double a, double b, double expected) {
            assertEquals(expected, service.add(a, b), DELTA);
        }
    }

    @Nested
    @DisplayName("subtract")
    class Subtract {

        @ParameterizedTest(name = "{0} - {1} = {2}")
        @CsvSource({
                "5, 3, 2",
                "3, 5, -2",
                "-3, -5, 2",
                "0, 0, 0",
                "2.75, 0.25, 2.5"
        })
        void subtractsTwoNumbers(double a, double b, double expected) {
            assertEquals(expected, service.subtract(a, b), DELTA);
        }
    }

    @Nested
    @DisplayName("multiply")
    class Multiply {

        @ParameterizedTest(name = "{0} * {1} = {2}")
        @CsvSource({
                "2, 3, 6",
                "-2, 3, -6",
                "-2, -3, 6",
                "7, 0, 0",
                "2.5, 4, 10"
        })
        void multipliesTwoNumbers(double a, double b, double expected) {
            assertEquals(expected, service.multiply(a, b), DELTA);
        }
    }

    @Nested
    @DisplayName("divide")
    class Divide {

        @ParameterizedTest(name = "{0} / {1} = {2}")
        @CsvSource({
                "6, 3, 2",
                "-6, 3, -2",
                "-6, -3, 2",
                "7, 2, 3.5",
                "0, 5, 0"
        })
        void dividesTwoNumbers(double a, double b, double expected) {
            assertEquals(expected, service.divide(a, b), DELTA);
        }

        @Test
        @DisplayName("throws DivisionByZeroException when the divisor is zero")
        void rejectsZeroDivisor() {
            DivisionByZeroException ex = assertThrows(
                    DivisionByZeroException.class, () -> service.divide(10, 0));
            assertEquals("Division by zero is not allowed. Give a non-zero value for 'b'.", ex.getMessage());
        }

        @Test
        @DisplayName("throws DivisionByZeroException when the divisor is negative zero")
        void rejectsNegativeZeroDivisor() {
            assertThrows(DivisionByZeroException.class, () -> service.divide(10, -0.0));
        }
    }

    @Nested
    @DisplayName("input validation")
    class Validation {

        @ParameterizedTest(name = "rejects a = {0}")
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        void rejectsNonFiniteFirstOperand(double a) {
            assertThrows(InvalidInputException.class, () -> service.add(a, 1));
        }

        @ParameterizedTest(name = "rejects b = {0}")
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        void rejectsNonFiniteSecondOperand(double b) {
            assertThrows(InvalidInputException.class, () -> service.multiply(1, b));
        }

        @Test
        @DisplayName("rejects a result that overflows the double range")
        void rejectsOverflowResult() {
            assertThrows(InvalidInputException.class,
                    () -> service.multiply(Double.MAX_VALUE, Double.MAX_VALUE));
        }
    }
}
