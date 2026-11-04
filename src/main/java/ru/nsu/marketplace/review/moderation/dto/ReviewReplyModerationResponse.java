package ru.nsu.marketplace.review.moderation.dto;

import ru.nsu.marketplace.review.reply.ReplyStatus;

import java.time.Instant;

public record ReviewReplyModerationResponse(
        Long id,
        Long authorId,
        Long reviewId,
        Long productId,
        ReplyStatus status,
        String authorName,
        boolean isSeller,
        Long replyToId,
        String text,
        Instant createdAt
) {
}
