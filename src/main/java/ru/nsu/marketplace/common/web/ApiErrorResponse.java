package ru.nsu.marketplace.common.web;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        int status,
        String message,
        List<FieldErrorResponse> errors,
        Instant timestamp
) {}
