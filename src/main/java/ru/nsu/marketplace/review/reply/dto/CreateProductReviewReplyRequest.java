package ru.nsu.marketplace.review.reply.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateProductReviewReplyRequest(
        @NotBlank
        @Size(max = 500)
        String text,
        @Positive Long replyToId
) {}
