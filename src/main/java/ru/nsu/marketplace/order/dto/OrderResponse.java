package ru.nsu.marketplace.order.dto;

import ru.nsu.marketplace.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        OrderStatus status,
        Instant createdAt,
        Instant deliveredAt,
        BigDecimal totalPrice,
        List<OrderItemResponse> items
) {}
