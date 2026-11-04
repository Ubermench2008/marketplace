package ru.nsu.marketplace.cart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.nsu.marketplace.cart.dto.CartResponse;
import ru.nsu.marketplace.cart.exception.CartItemNotFoundException;
import ru.nsu.marketplace.cart.exception.CartNotFoundException;
import ru.nsu.marketplace.product.ProductCardRepository;
import ru.nsu.marketplace.product.ProductRepository;
import ru.nsu.marketplace.product.ProductService;
import ru.nsu.marketplace.product.dto.ProductCardResponse;
import ru.nsu.marketplace.product.exception.ProductNotFoundException;
import ru.nsu.marketplace.product.model.ProductCardEntity;
import ru.nsu.marketplace.product.model.ProductEntity;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductCardRepository productCardRepository;

    @Mock
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService service;

    @Test
    void shouldReturnEmptyCartWithZeroTotalPrice() {
        when(cartRepository.findByUser_Id(10L)).thenReturn(Optional.of(new CartEntity()));

        CartResponse response = service.getCart(10L);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        verifyNoInteractions(productCardRepository, productService);
    }

    @Test
    void shouldReturnItemsWithSubtotalsAndTotalPrice() {
        CartEntity cart = new CartEntity();
        cart.getItems().add(item(1L, "19.90", 3));
        cart.getItems().add(item(2L, "5.25", 2));
        when(cartRepository.findByUser_Id(10L)).thenReturn(Optional.of(cart));

        for (CartItemEntity item : cart.getItems()) {
            ProductCardEntity card = new ProductCardEntity();
            card.setId(100L + item.getId());
            card.setProduct(item.getProduct());
            card.setPreviewImageUrl("/media/preview.webp");

            ProductCardResponse cardResponse = new ProductCardResponse(
                item.getProduct().getPublicId(),
                "product-slug",
                "Product",
                item.getProduct().getPrice(),
                card.getPreviewImageUrl(),
                item.getProduct().getStatus()
            );
            when(productCardRepository.findByProduct_PublicId(item.getProduct().getPublicId())).thenReturn(
                Optional.of(card)
            );
            when(productService.toCardResponse(card)).thenReturn(cardResponse);
        }

        CartResponse response = service.getCart(10L);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).id()).isEqualTo(1L);
        assertThat(response.items().get(0).quantity()).isEqualTo(3);
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo("59.70");
        assertThat(response.items().get(1).subtotal()).isEqualByComparingTo("10.50");
        assertThat(response.totalPrice()).isEqualByComparingTo("70.20");

        for (int i = 0; i < response.items().size(); i++) {
            UUID productId = cart.getItems().get(i).getProduct().getPublicId();
            assertThat(response.items().get(i).product().id()).isEqualTo(productId);
            verify(productCardRepository).findByProduct_PublicId(productId);
        }
    }

    @Test
    void shouldThrowWhenUserCartIsMissing() {
        when(cartRepository.findByUser_Id(10L)).thenReturn(Optional.empty());

        CartNotFoundException exception = assertThrows(CartNotFoundException.class, () ->
            service.getCart(10L)
        );

        assertThat(exception.getMessage()).isEqualTo("User cart is missing: userId=10");
        verifyNoInteractions(productCardRepository, productService);
    }

    @Test
    void shouldThrowWhenProductCardIsMissing() {
        CartEntity cart = new CartEntity();
        cart.getItems().add(item(1L, "19.90", 1));
        when(cartRepository.findByUser_Id(10L)).thenReturn(Optional.of(cart));

        assertThrows(IllegalStateException.class, () -> service.getCart(10L));

        verifyNoInteractions(productService);
    }

    @Test
    void shouldAddNewProductWithQuantityOne() {
        CartEntity cart = new CartEntity();
        CartItemEntity existingItem = item(1L, "10.00", 2);
        cart.getItems().add(existingItem);
        ProductEntity product = item(2L, "19.90", 1).getProduct();
        UUID productId = product.getPublicId();
        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        service.addProductToCart(10L, productId);

        assertThat(cart.getItems()).hasSize(2);
        CartItemEntity addedItem = cart.getItems().get(1);
        assertThat(addedItem.getProduct()).isSameAs(product);
        assertThat(addedItem.getCart()).isSameAs(cart);
        assertThat(addedItem.getQuantity()).isEqualTo(1);
        assertThat(cart.getItems().getFirst()).isSameAs(existingItem);
        assertThat(existingItem.getQuantity()).isEqualTo(2);
    }

    @Test
    void shouldIncreaseQuantityWithoutCreatingDuplicateItem() {
        CartEntity cart = new CartEntity();
        CartItemEntity existingItem = item(1L, "19.90", 3);
        CartItemEntity otherItem = item(2L, "5.25", 2);
        existingItem.setCart(cart);
        otherItem.setCart(cart);
        cart.getItems().add(otherItem);
        cart.getItems().add(existingItem);
        UUID productId = existingItem.getProduct().getPublicId();
        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(existingItem.getProduct()));
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        service.addProductToCart(10L, productId);

        assertThat(cart.getItems()).containsExactly(otherItem, existingItem);
        assertThat(existingItem.getQuantity()).isEqualTo(4);
        assertThat(otherItem.getQuantity()).isEqualTo(2);
    }

    @Test
    void shouldThrowWhenAddingMissingProduct() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findByPublicId(productId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> service.addProductToCart(10L, productId));

        verifyNoInteractions(cartRepository, productCardRepository, productService);
    }

    @Test
    void shouldThrowWhenAddingProductToMissingCart() {
        ProductEntity product = item(1L, "19.90", 1).getProduct();
        UUID productId = product.getPublicId();
        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> service.addProductToCart(10L, productId));

        verifyNoInteractions(productCardRepository, productService);
    }

    @Test
    void shouldClearAllCartItems() {
        CartEntity cart = new CartEntity();
        cart.getItems().add(item(1L, "19.90", 3));
        cart.getItems().add(item(2L, "5.25", 2));
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        service.clearCart(10L);

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository, never()).delete(cart);
        verifyNoInteractions(productRepository, productCardRepository, productService);
    }

    @Test
    void shouldAllowClearingAlreadyEmptyCart() {
        CartEntity cart = new CartEntity();
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        service.clearCart(10L);
        service.clearCart(10L);

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository, never()).delete(cart);
        verifyNoInteractions(productRepository, productCardRepository, productService);
    }

    @Test
    void shouldThrowWhenClearingMissingCart() {
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> service.clearCart(10L));

        verifyNoInteractions(productRepository, productCardRepository, productService);
    }

    @Test
    void shouldDeleteOnlyRequestedCartItem() {
        CartEntity cart = new CartEntity();
        CartItemEntity deletedItem = item(1L, "19.90", 3);
        CartItemEntity remainingItem = item(2L, "5.25", 2);
        cart.getItems().add(deletedItem);
        cart.getItems().add(remainingItem);
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        service.deleteCartItem(10L, 1L);

        assertThat(cart.getItems()).containsExactly(remainingItem);
        assertThat(remainingItem.getQuantity()).isEqualTo(2);
        verify(cartRepository, never()).delete(cart);
        verifyNoInteractions(productRepository, productCardRepository, productService);
    }

    @Test
    void shouldNotDeleteItemOutsideCurrentUserCart() {
        CartEntity cart = new CartEntity();
        CartItemEntity existingItem = item(1L, "19.90", 1);
        cart.getItems().add(existingItem);
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        assertThrows(CartItemNotFoundException.class, () -> service.deleteCartItem(10L, 2L));

        assertThat(cart.getItems()).containsExactly(existingItem);
        verifyNoInteractions(productRepository, productCardRepository, productService);
    }

    @Test
    void shouldThrowWhenDeletingItemFromMissingCart() {
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> service.deleteCartItem(10L, 1L));

        verifyNoInteractions(productRepository, productCardRepository, productService);
    }

    @Test
    void shouldSetQuantityOnlyForRequestedCartItem() {
        CartEntity cart = new CartEntity();
        CartItemEntity updatedItem = item(1L, "19.90", 3);
        CartItemEntity otherItem = item(2L, "5.25", 2);
        cart.getItems().add(updatedItem);
        cart.getItems().add(otherItem);
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        service.updateCartItemQuantity(10L, 1L, 5);

        assertThat(updatedItem.getQuantity()).isEqualTo(5);
        assertThat(otherItem.getQuantity()).isEqualTo(2);
        assertThat(cart.getItems()).containsExactly(updatedItem, otherItem);
        verifyNoInteractions(productRepository, productCardRepository, productService);
    }

    @Test
    void shouldNotUpdateItemOutsideCurrentUserCart() {
        CartEntity cart = new CartEntity();
        CartItemEntity existingItem = item(1L, "19.90", 3);
        cart.getItems().add(existingItem);
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        assertThrows(CartItemNotFoundException.class, () -> service.updateCartItemQuantity(10L, 2L, 5));

        assertThat(existingItem.getQuantity()).isEqualTo(3);
    }

    @Test
    void shouldThrowWhenUpdatingQuantityInMissingCart() {
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> service.updateCartItemQuantity(10L, 1L, 5));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = { 0, -1 })
    void shouldRejectInvalidQuantity(Integer quantity) {
        assertThrows(IllegalArgumentException.class, () -> service.updateCartItemQuantity(10L, 1L, quantity));

        verifyNoInteractions(cartRepository, productRepository, productCardRepository, productService);
    }

    private CartItemEntity item(Long id, String price, int quantity) {
        ProductEntity product = ProductEntity.builder()
            .id(id + 10L)
            .publicId(UUID.randomUUID())
            .price(new BigDecimal(price))
            .build();

        CartItemEntity item = new CartItemEntity();
        item.setId(id);
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }
}
