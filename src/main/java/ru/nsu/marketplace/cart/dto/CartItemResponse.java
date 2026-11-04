package ru.nsu.marketplace.cart.dto;

import ru.nsu.marketplace.product.dto.ProductCardResponse;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        ProductCardResponse product,
        int quantity,
        BigDecimal subtotal
) {}
