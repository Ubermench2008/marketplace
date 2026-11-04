package ru.nsu.marketplace.product.dto;

import ru.nsu.marketplace.product.model.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCardResponse(
        UUID id,
        String slug,
        String name,
        BigDecimal price,
        String imgUrl,
        ProductStatus status
) {}
