package ru.nsu.marketplace.product.moderation.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductModerationSnapshotResponse(
        String name,
        String slug,
        String description,
        BigDecimal price,
        String previewImageUrl,
        List<String> detailsImageUrls
) {}
