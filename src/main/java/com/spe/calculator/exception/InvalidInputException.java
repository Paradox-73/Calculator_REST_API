package com.spe.calculator.exception;

/**
 * Thrown when an operand is not a finite number, or when a result is out of range.
 */
public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }
}
