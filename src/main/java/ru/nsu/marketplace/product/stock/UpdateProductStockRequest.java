package ru.nsu.marketplace.product.stock;

import jakarta.validation.constraints.NotNull;

public record UpdateProductStockRequest(
        @NotNull Integer delta
) {}
