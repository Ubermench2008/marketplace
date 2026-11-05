package ru.nsu.marketplace.cart;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.nsu.marketplace.cart.dto.CartResponse;
import ru.nsu.marketplace.cart.dto.AddCartItemRequest;
import ru.nsu.marketplace.cart.dto.UpdateCartItemQuantityRequest;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER', 'SELLER') and !hasRole('ADMIN')")
public class CartController {
    private final CartService cartService;

    @GetMapping
    public CartResponse getMyCart(@AuthenticationPrincipal Long userId) {
        return cartService.getCart(userId);
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addProductToCart(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid AddCartItemRequest request
    ) {
        cartService.addProductToCart(userId, request.productId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteCartItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long itemId
    ) {
        cartService.deleteCartItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<Void> updateCartItemQuantity(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid UpdateCartItemQuantityRequest request
    ) {
        cartService.updateCartItemQuantity(userId, itemId, request.quantity());
        return ResponseEntity.noContent().build();
    }
}
