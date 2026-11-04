package ru.nsu.marketplace.review.reaction;

import jakarta.validation.constraints.NotNull;

public record SetReactionRequest(
        @NotNull ReactionType type
) {}
