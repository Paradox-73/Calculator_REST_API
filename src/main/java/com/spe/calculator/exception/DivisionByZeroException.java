package com.spe.calculator.exception;

/**
 * Thrown when a divide request uses zero as the divisor.
 */
public class DivisionByZeroException extends RuntimeException {

    public DivisionByZeroException(String message) {
        super(message);
    }
}
