package ru.nsu.marketplace.review.dto;

import ru.nsu.marketplace.common.moderation.ModerationStatus;

public record MyProductReviewResponse(
        ProductReviewResponse review,
        ModerationStatus moderationStatus,
        long reviewVersion,
        String rejectionReason
) {}
