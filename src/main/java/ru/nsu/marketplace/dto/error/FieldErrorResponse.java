package ru.nsu.marketplace.dto.error;

public record FieldErrorResponse(
        String field,
        String message
) {}
