package ru.nsu.marketplace.common.web;

public record FieldErrorResponse(
        String field,
        String message
) {}
