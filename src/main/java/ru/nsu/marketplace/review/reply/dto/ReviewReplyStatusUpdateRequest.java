package ru.nsu.marketplace.review.reply.dto;

import ru.nsu.marketplace.review.reply.ReplyStatus;

public record ReviewReplyStatusUpdateRequest(
        ReplyStatus status
) {
}
