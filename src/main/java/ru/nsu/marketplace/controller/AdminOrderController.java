package ru.nsu.marketplace.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.nsu.marketplace.dto.OrderResponse;
import ru.nsu.marketplace.service.OrderService;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {
    private final OrderService service;

    @GetMapping("/users/{userId}/orders")
    public Page<OrderResponse> getUserOrders(@PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.getOrdersByUser(userId, pageable);
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        return service.getOrder(orderId);
    }

    @PatchMapping("/orders/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long orderId) {
        return service.cancelOrderByAdmin(orderId);
    }

    @PatchMapping("/orders/{orderId}/deliver")
    public OrderResponse markOrderDelivered(@PathVariable Long orderId) {
        return service.markOrderDelivered(orderId);
    }
}
