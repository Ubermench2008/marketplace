package ru.nsu.marketplace.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductDetailsResponse(
        UUID id,
        String name,
        String slug,
        BigDecimal price,
        String description,
        List<String> imageUrlList
) {}
