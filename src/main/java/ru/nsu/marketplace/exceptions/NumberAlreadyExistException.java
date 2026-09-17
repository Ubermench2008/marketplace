package ru.nsu.marketplace.exceptions;

public class NumberAlreadyExistException extends RuntimeException {
    public NumberAlreadyExistException(String message) {
        super(message);
    }
}
