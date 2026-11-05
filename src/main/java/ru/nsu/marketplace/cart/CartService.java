package ru.nsu.marketplace.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.cart.dto.CartItemResponse;
import ru.nsu.marketplace.cart.dto.CartResponse;
import ru.nsu.marketplace.cart.exception.CartItemNotFoundException;
import ru.nsu.marketplace.cart.exception.CartNotFoundException;
import ru.nsu.marketplace.order.OrderItemEntity;
import ru.nsu.marketplace.product.ProductCardRepository;
import ru.nsu.marketplace.product.ProductRepository;
import ru.nsu.marketplace.product.ProductService;
import ru.nsu.marketplace.product.exception.ProductNotFoundException;
import ru.nsu.marketplace.product.model.ProductCardEntity;
import ru.nsu.marketplace.product.model.ProductEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {
    private final ProductService productService;
    private final CartRepository cartRepository;
    private final ProductCardRepository productCardRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        CartEntity cart = cartRepository.findByUser_Id(userId).orElseThrow(
                () -> new CartNotFoundException("User cart is missing: userId=" + userId)
        );
        return toResponse(cart);
    }

    @Transactional
    public void addProductToCart(Long userId, UUID productId) {
        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product is missing: product=" + productId)
        );

        CartEntity cart = findCartForUpdate(userId);

        Optional<CartItemEntity> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getPublicId().equals(productId))
                .findFirst();

        if (existingItem.isEmpty()) {
            CartItemEntity cartItem = new CartItemEntity();
            cartItem.setProduct(product);
            cartItem.setCart(cart);
            cartItem.setQuantity(1);

            cart.getItems().add(cartItem);
        } else {
            CartItemEntity cartItem = existingItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + 1);
        }
    }

    @Transactional
    public void clearCart(Long userId) {
        CartEntity cart = findCartForUpdate(userId);

        cart.getItems().clear();
    }

    @Transactional
    public void deleteCartItem(Long userId, Long itemId) {
        CartEntity cart = findCartForUpdate(userId);

        CartItemEntity cartItem = findCartItem(cart, itemId);

        cart.getItems().remove(cartItem);
    }

    @Transactional
    public void updateCartItemQuantity(Long userId, Long itemId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        CartEntity cart = findCartForUpdate(userId);

        CartItemEntity cartItem = findCartItem(cart, itemId);

        cartItem.setQuantity(quantity);
    }

    private CartEntity findCartForUpdate(Long userId) {
        return cartRepository.findByUserIdForUpdate(userId).orElseThrow(
                () -> new CartNotFoundException("User cart is missing: userId=" + userId)
        );
    }

    private CartItemEntity findCartItem(CartEntity cart, Long itemId) {
        return cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found: itemId=" + itemId));
    }

    @Transactional
    public void restoreUserCartFromItems(List<OrderItemEntity> items, Long userId) {
        CartEntity cart = findCartForUpdate(userId);

        for (OrderItemEntity orderItem : items) {
            UUID productId = orderItem.getProduct().getPublicId();
            Optional<CartItemEntity> existingItem = cart.getItems().stream()
                    .filter(item -> item.getProduct().getPublicId().equals(productId))
                    .findFirst();

            if (existingItem.isPresent()) {
                CartItemEntity cartItem = existingItem.get();
                cartItem.setQuantity(cartItem.getQuantity() + orderItem.getQuantity());
            } else {
                cart.getItems().add(toCartItem(orderItem, cart));
            }
        }
    }

    private CartItemEntity toCartItem(OrderItemEntity orderItem, CartEntity cart) {
        CartItemEntity cartItem = new CartItemEntity();
        cartItem.setCart(cart);
        cartItem.setProduct(orderItem.getProduct());
        cartItem.setQuantity(orderItem.getQuantity());
        return cartItem;
    }

    public CartResponse toResponse(CartEntity cart) {
        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItemEntity item : cart.getItems()) {
            int quantity = item.getQuantity();
            BigDecimal price = item.getProduct().getPrice();
            BigDecimal subtotal = new BigDecimal(quantity).multiply(price);

            ProductCardEntity card = productCardRepository
                    .findByProduct_PublicId(item.getProduct().getPublicId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Product card is missing: productId=" + item.getProduct().getPublicId()
                    ));

            items.add(new CartItemResponse(
                    item.getId(),
                    productService.toCardResponse(card),
                    item.getQuantity(),
                    subtotal
            ));

            totalPrice = totalPrice.add(subtotal);
        }

        return new CartResponse(items, totalPrice);
    }

}
