package ru.nsu.marketplace.review.exception;

public class ProductReviewNotFoundException extends RuntimeException {
    public ProductReviewNotFoundException(String message) {
        super(message);
    }
}
