package ru.nsu.marketplace.review.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductReviewResponse(
        Long id,
        UUID productId,
        Long userId,
        String userName,
        int rating,
        String text,
        long likes,
        long dislikes,
        Instant createdAt,
        long replyCount,
        boolean deleted
) {}
