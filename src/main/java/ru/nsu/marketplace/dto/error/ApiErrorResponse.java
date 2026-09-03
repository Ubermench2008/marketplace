package ru.nsu.marketplace.dto.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        int status,
        String message,
        List<FieldErrorResponse> errors,
        Instant timestamp
) {}
