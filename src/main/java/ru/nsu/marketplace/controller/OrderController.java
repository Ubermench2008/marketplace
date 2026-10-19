package ru.nsu.marketplace.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.nsu.marketplace.dto.OrderResponse;
import ru.nsu.marketplace.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER', 'SELLER') and !hasRole('ADMIN')")
public class OrderController {
    private final OrderService service;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createOrder(userId));
    }

    @GetMapping("/current")
    public Page<OrderResponse> getCurrentOrders(@AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.getCurrentOrders(userId, pageable);
    }

    @GetMapping("/history")
    public Page<OrderResponse> getOrderHistory(@AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.getOrderHistory(userId, pageable);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@AuthenticationPrincipal Long userId, @PathVariable Long orderId) {
        return service.getUserOrder(userId, orderId);
    }

    @PatchMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@AuthenticationPrincipal Long userId, @PathVariable Long orderId) {
        return service.cancelUserOrder(userId, orderId);
    }
}
