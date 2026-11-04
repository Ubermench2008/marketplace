package ru.nsu.marketplace.review.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RejectReviewModerationRequest(
        @NotNull
        @PositiveOrZero
        Long reviewVersion,

        @NotBlank
        @Size(max = 2000)
        String reason
) {}
