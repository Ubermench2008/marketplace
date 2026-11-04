package ru.nsu.marketplace.product.exception;

public class ModerationRequestNotFoundException extends RuntimeException {
    public ModerationRequestNotFoundException(String message) {
        super(message);
    }
}
