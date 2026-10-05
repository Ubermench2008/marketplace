package ru.nsu.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.dto.CartItemResponse;
import ru.nsu.marketplace.dto.CartResponse;
import ru.nsu.marketplace.entity.CartEntity;
import ru.nsu.marketplace.entity.CartItemEntity;
import ru.nsu.marketplace.entity.ProductCardEntity;
import ru.nsu.marketplace.entity.ProductEntity;
import ru.nsu.marketplace.exceptions.ProductNotFoundException;
import ru.nsu.marketplace.exceptions.CartNotFoundException;
import ru.nsu.marketplace.repository.CartRepository;
import ru.nsu.marketplace.repository.ProductCardsRepository;
import ru.nsu.marketplace.repository.ProductRepository;

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
    private final ProductCardsRepository productCardsRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        CartEntity cart = cartRepository.findByUser_Id(userId).orElseThrow(
                () -> new CartNotFoundException("User cart is missing: userId=" + userId)
        );
        return toDto(cart);
    }

    @Transactional
    public void addProductInCart(Long userId, UUID productId) {
        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product is missing: product=" + productId)
        );

        CartEntity cart = cartRepository.findByUser_Id(userId).orElseThrow(
                () -> new CartNotFoundException("User cart is missing: userId=" + userId)
        );

        Optional<CartItemEntity> cartItemEntity = cart.getCartItemEntities().stream().
                filter(item -> item.getProduct().getPublicId().equals(productId))
                .findFirst();

        if (cartItemEntity.isEmpty()) {
            CartItemEntity cartItem = new CartItemEntity();
            cartItem.setProduct(product);
            cartItem.setCart(cart);
            cartItem.setQuantity(1);

            cart.getCartItemEntities().add(cartItem);
        } else {
            CartItemEntity cartItem = cartItemEntity.get();
            cartItem.setQuantity(cartItem.getQuantity() + 1);
        }
    }

    @Transactional
    public void clearCart(Long userId) {
        CartEntity cart = cartRepository.findByUser_Id(userId).orElseThrow(
                () -> new CartNotFoundException("User cart is missing: userId=" + userId)
        );

        cart.getCartItemEntities().clear();
    }

    public CartResponse toDto(CartEntity cartEntity) {
        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItemEntity entity : cartEntity.getCartItemEntities()) {
            int quantity = entity.getQuantity();
            BigDecimal price = entity.getProduct().getPrice();
            BigDecimal subtotal = new BigDecimal(quantity).multiply(price);

            ProductCardEntity card = productCardsRepository
                    .findByProduct_PublicId(entity.getProduct().getPublicId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Product card is missing: productId=" + entity.getProduct().getPublicId()
                    ));

            items.add(new CartItemResponse(
                    entity.getId(),
                    productService.toCardResponse(card),
                    entity.getQuantity(),
                    subtotal
            ));

            totalPrice = totalPrice.add(subtotal);
        }

        return new CartResponse(items, totalPrice);
    }


}
