package ru.nsu.marketplace.product.stock;

import java.util.UUID;

public record ProductStockResponse(
        UUID productId,
        int quantity
) {}
