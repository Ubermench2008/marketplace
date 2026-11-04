package ru.nsu.marketplace.product.exception;

public class ProductModerationStateException extends RuntimeException {
    public ProductModerationStateException(String message) {
        super(message);
    }
}
