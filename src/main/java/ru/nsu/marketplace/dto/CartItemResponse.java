package ru.nsu.marketplace.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        ProductCardResponse product,
        int quantity,
        BigDecimal subtotal
) {}
