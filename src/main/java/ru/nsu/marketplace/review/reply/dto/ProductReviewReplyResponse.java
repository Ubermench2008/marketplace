package ru.nsu.marketplace.review.reply.dto;

import java.time.Instant;
import ru.nsu.marketplace.review.reaction.ReactionType;
import ru.nsu.marketplace.review.reply.ReplyStatus;

public record ProductReviewReplyResponse(
        Long id,
        Long authorId,
        Long reviewId,
        String authorName,
        boolean isSeller,
        Long replyToId,
        String text,
        Instant createdAt,
        long likes,
        long dislikes,
        ReactionType myReaction,
        long replyCount,
        ReplyStatus status
) {}
