package ru.nsu.marketplace.review.exception;

public class ReviewReplyNotFoundException extends RuntimeException {
    public ReviewReplyNotFoundException(String message) {
        super(message);
    }
}
