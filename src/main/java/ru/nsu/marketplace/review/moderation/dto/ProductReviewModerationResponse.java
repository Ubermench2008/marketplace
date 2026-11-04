package ru.nsu.marketplace.review.moderation.dto;

import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.common.moderation.ReviewerResponse;

import java.time.Instant;
import java.util.UUID;

public record ProductReviewModerationResponse(
        UUID publicId,
        Long reviewId,
        UUID productId,
        Long userId,
        String userName,
        int rating,
        String text,
        long reviewVersion,
        ModerationStatus status,
        Instant submittedAt,
        Instant reviewedAt,
        ReviewerResponse reviewedBy,
        String rejectionReason
) {}
