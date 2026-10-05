package ru.nsu.marketplace.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import ru.nsu.marketplace.dto.CartResponse;
import ru.nsu.marketplace.service.CartService;

import java.util.UUID;

@RestController()
@RequestMapping("/api/cart")
@RequiredArgsConstructor

public class CartController {
    private final CartService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER') and !hasRole('ADMIN')")
    public CartResponse getCart(@AuthenticationPrincipal Long userId) {
        return service.getCart(userId);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public CartResponse getCartByUserId(@PathVariable("userId") Long userId) {
        return service.getCart(userId);
    }

    @PostMapping("/{productId}")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER') and !hasRole('ADMIN')")
    public ResponseEntity<Void> addProductInCart(@AuthenticationPrincipal Long userId, @PathVariable("productId") UUID productId) {
        service.addProductInCart(userId, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER') and !hasRole('ADMIN')")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal Long userId) {
        service.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
