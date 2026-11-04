package ru.nsu.marketplace.product.dto;

import java.util.UUID;

public record ProductDetailsImageResponse(
        UUID uuid,
        String imgUrl,
        int position
) {}
