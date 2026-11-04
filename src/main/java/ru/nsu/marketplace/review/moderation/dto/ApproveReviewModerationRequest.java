package ru.nsu.marketplace.review.moderation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ApproveReviewModerationRequest(
        @NotNull
        @PositiveOrZero
        Long reviewVersion
) {}
