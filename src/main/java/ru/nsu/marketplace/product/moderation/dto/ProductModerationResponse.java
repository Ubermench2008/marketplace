package ru.nsu.marketplace.product.moderation.dto;

import lombok.Builder;
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.common.moderation.ReviewerResponse;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ProductModerationResponse(
        UUID publicId,
        UUID productId,
        ModerationStatus status,
        Instant submittedAt,
        Instant reviewedAt,
        ReviewerResponse reviewedBy,
        String rejectionReason,
        ProductModerationSnapshotResponse snapshot
) {}
