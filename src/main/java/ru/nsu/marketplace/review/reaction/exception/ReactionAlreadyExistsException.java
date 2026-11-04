package ru.nsu.marketplace.review.reaction.exception;

public class ReactionAlreadyExistsException extends RuntimeException {
    public ReactionAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
