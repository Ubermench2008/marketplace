package ru.nsu.marketplace.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        Long id,
        UUID productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
