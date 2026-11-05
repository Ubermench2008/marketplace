package ru.nsu.marketplace.order;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.nsu.marketplace.order.dto.OrderResponse;

import java.net.URI;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER', 'SELLER') and !hasRole('ADMIN')")
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@AuthenticationPrincipal Long userId) {
        OrderResponse order = orderService.createOrder(userId);
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }

    @GetMapping
    public Page<OrderResponse> getOrders(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "CURRENT") View view,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return switch (view) {
            case CURRENT -> orderService.getCurrentOrders(userId, pageable);
            case HISTORY -> orderService.getOrderHistory(userId, pageable);
        };
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@AuthenticationPrincipal Long userId, @PathVariable Long orderId) {
        return orderService.getUserOrder(userId, orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@AuthenticationPrincipal Long userId, @PathVariable Long orderId) {
        return orderService.cancelUserOrder(userId, orderId);
    }

    public enum View {
        CURRENT,
        HISTORY
    }
}
