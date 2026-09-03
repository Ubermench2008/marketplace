package ru.nsu.marketplace.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCardResponse(
        UUID id,
        String slug,
        String name,
        BigDecimal price,
        String imgUrl
) {}
