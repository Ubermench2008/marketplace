package ru.nsu.marketplace.user.exception;

public class TelephoneAlreadyExistsException extends RuntimeException {
    public TelephoneAlreadyExistsException(String message) {
        super(message);
    }
}
