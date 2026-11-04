package ru.nsu.marketplace.product.dto;

import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.product.model.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductDetailsResponse(
        UUID id,
        String name,
        String slug,
        BigDecimal price,
        String description,
        List<String> imageUrlList,
        String previewImageUrl,
        ProductStatus status,
        ModerationStatus moderationStatus
) {}
