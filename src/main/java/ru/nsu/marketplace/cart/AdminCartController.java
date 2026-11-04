package ru.nsu.marketplace.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.marketplace.cart.dto.CartResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCartController {
    private final CartService cartService;

    @GetMapping("/{userId}/cart")
    public CartResponse getUserCart(@PathVariable Long userId) {
        return cartService.getCart(userId);
    }
}
