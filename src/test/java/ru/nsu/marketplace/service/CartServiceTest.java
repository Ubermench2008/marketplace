package ru.nsu.marketplace.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.nsu.marketplace.dto.CartResponse;
import ru.nsu.marketplace.dto.ProductCardResponse;
import ru.nsu.marketplace.entity.CartEntity;
import ru.nsu.marketplace.entity.CartItemEntity;
import ru.nsu.marketplace.entity.ProductCardEntity;
import ru.nsu.marketplace.entity.ProductEntity;
import ru.nsu.marketplace.exceptions.CartNotFoundException;
import ru.nsu.marketplace.exceptions.CartItemNotFoundException;
import ru.nsu.marketplace.exceptions.ProductNotFoundException;
import ru.nsu.marketplace.repository.CartRepository;
import ru.nsu.marketplace.repository.ProductCardsRepository;
import ru.nsu.marketplace.repository.ProductRepository;

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
    private ProductCardsRepository productCardsRepository;

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
        verifyNoInteractions(productCardsRepository, productService);
    }

    @Test
    void shouldReturnItemsWithSubtotalsAndTotalPrice() {
        CartEntity cart = new CartEntity();
        cart.getCartItemEntities().add(item(1L, "19.90", 3));
        cart.getCartItemEntities().add(item(2L, "5.25", 2));
        when(cartRepository.findByUser_Id(10L)).thenReturn(Optional.of(cart));

        for (CartItemEntity item : cart.getCartItemEntities()) {
            ProductCardEntity card = new ProductCardEntity();
            card.setId(100L + item.getId());
            card.setProduct(item.getProduct());
            card.setImgUrl("/media/preview.webp");

            ProductCardResponse cardResponse = new ProductCardResponse(
                    item.getProduct().getPublicId(), "product-slug", "Product",
                    item.getProduct().getPrice(), card.getImgUrl()
            );
            when(productCardsRepository.findByProduct_PublicId(item.getProduct().getPublicId()))
                    .thenReturn(Optional.of(card));
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
            UUID productId = cart.getCartItemEntities().get(i).getProduct().getPublicId();
            assertThat(response.items().get(i).product().id()).isEqualTo(productId);
            verify(productCardsRepository).findByProduct_PublicId(productId);
        }
    }

    @Test
    void shouldThrowWhenUserCartIsMissing() {
        when(cartRepository.findByUser_Id(10L)).thenReturn(Optional.empty());

        CartNotFoundException exception = assertThrows(CartNotFoundException.class,
                () -> service.getCart(10L));

        assertThat(exception.getMessage()).isEqualTo("User cart is missing: userId=10");
        verifyNoInteractions(productCardsRepository, productService);
    }

    @Test
    void shouldThrowWhenProductCardIsMissing() {
        CartEntity cart = new CartEntity();
        cart.getCartItemEntities().add(item(1L, "19.90", 1));
        when(cartRepository.findByUser_Id(10L)).thenReturn(Optional.of(cart));

        assertThrows(IllegalStateException.class, () -> service.getCart(10L));

        verifyNoInteractions(productService);
    }

    @Test
    void shouldAddNewProductWithQuantityOne() {
        CartEntity cart = new CartEntity();
        CartItemEntity existingItem = item(1L, "10.00", 2);
        cart.getCartItemEntities().add(existingItem);
        ProductEntity product = item(2L, "19.90", 1).getProduct();
        UUID productId = product.getPublicId();
        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        service.addProductInCart(10L, productId);

        assertThat(cart.getCartItemEntities()).hasSize(2);
        CartItemEntity addedItem = cart.getCartItemEntities().get(1);
        assertThat(addedItem.getProduct()).isSameAs(product);
        assertThat(addedItem.getCart()).isSameAs(cart);
        assertThat(addedItem.getQuantity()).isEqualTo(1);
        assertThat(cart.getCartItemEntities().getFirst()).isSameAs(existingItem);
        assertThat(existingItem.getQuantity()).isEqualTo(2);
    }

    @Test
    void shouldIncreaseQuantityWithoutCreatingDuplicateItem() {
        CartEntity cart = new CartEntity();
        CartItemEntity existingItem = item(1L, "19.90", 3);
        CartItemEntity otherItem = item(2L, "5.25", 2);
        existingItem.setCart(cart);
        otherItem.setCart(cart);
        cart.getCartItemEntities().add(otherItem);
        cart.getCartItemEntities().add(existingItem);
        UUID productId = existingItem.getProduct().getPublicId();
        when(productRepository.findByPublicId(productId))
                .thenReturn(Optional.of(existingItem.getProduct()));
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        service.addProductInCart(10L, productId);

        assertThat(cart.getCartItemEntities()).containsExactly(otherItem, existingItem);
        assertThat(existingItem.getQuantity()).isEqualTo(4);
        assertThat(otherItem.getQuantity()).isEqualTo(2);
    }

    @Test
    void shouldThrowWhenAddingMissingProduct() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findByPublicId(productId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> service.addProductInCart(10L, productId));

        verifyNoInteractions(cartRepository, productCardsRepository, productService);
    }

    @Test
    void shouldThrowWhenAddingProductToMissingCart() {
        ProductEntity product = item(1L, "19.90", 1).getProduct();
        UUID productId = product.getPublicId();
        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class,
                () -> service.addProductInCart(10L, productId));

        verifyNoInteractions(productCardsRepository, productService);
    }

    @Test
    void shouldClearAllCartItems() {
        CartEntity cart = new CartEntity();
        cart.getCartItemEntities().add(item(1L, "19.90", 3));
        cart.getCartItemEntities().add(item(2L, "5.25", 2));
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        service.clearCart(10L);

        assertThat(cart.getCartItemEntities()).isEmpty();
        verify(cartRepository, never()).delete(cart);
        verifyNoInteractions(productRepository, productCardsRepository, productService);
    }

    @Test
    void shouldAllowClearingAlreadyEmptyCart() {
        CartEntity cart = new CartEntity();
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        service.clearCart(10L);
        service.clearCart(10L);

        assertThat(cart.getCartItemEntities()).isEmpty();
        verify(cartRepository, never()).delete(cart);
        verifyNoInteractions(productRepository, productCardsRepository, productService);
    }

    @Test
    void shouldThrowWhenClearingMissingCart() {
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> service.clearCart(10L));

        verifyNoInteractions(productRepository, productCardsRepository, productService);
    }

    @Test
    void shouldDeleteOnlyRequestedCartItem() {
        CartEntity cart = new CartEntity();
        CartItemEntity deletedItem = item(1L, "19.90", 3);
        CartItemEntity remainingItem = item(2L, "5.25", 2);
        cart.getCartItemEntities().add(deletedItem);
        cart.getCartItemEntities().add(remainingItem);
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        service.deleteCartItem(10L, 1L);

        assertThat(cart.getCartItemEntities()).containsExactly(remainingItem);
        assertThat(remainingItem.getQuantity()).isEqualTo(2);
        verify(cartRepository, never()).delete(cart);
        verifyNoInteractions(productRepository, productCardsRepository, productService);
    }

    @Test
    void shouldNotDeleteItemOutsideCurrentUserCart() {
        CartEntity cart = new CartEntity();
        CartItemEntity existingItem = item(1L, "19.90", 1);
        cart.getCartItemEntities().add(existingItem);
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        assertThrows(CartItemNotFoundException.class, () -> service.deleteCartItem(10L, 2L));

        assertThat(cart.getCartItemEntities()).containsExactly(existingItem);
        verifyNoInteractions(productRepository, productCardsRepository, productService);
    }

    @Test
    void shouldThrowWhenDeletingItemFromMissingCart() {
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> service.deleteCartItem(10L, 1L));

        verifyNoInteractions(productRepository, productCardsRepository, productService);
    }

    @Test
    void shouldSetQuantityOnlyForRequestedCartItem() {
        CartEntity cart = new CartEntity();
        CartItemEntity updatedItem = item(1L, "19.90", 3);
        CartItemEntity otherItem = item(2L, "5.25", 2);
        cart.getCartItemEntities().add(updatedItem);
        cart.getCartItemEntities().add(otherItem);
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        service.updateQuantity(10L, 1L, 5);

        assertThat(updatedItem.getQuantity()).isEqualTo(5);
        assertThat(otherItem.getQuantity()).isEqualTo(2);
        assertThat(cart.getCartItemEntities()).containsExactly(updatedItem, otherItem);
        verifyNoInteractions(productRepository, productCardsRepository, productService);
    }

    @Test
    void shouldNotUpdateItemOutsideCurrentUserCart() {
        CartEntity cart = new CartEntity();
        CartItemEntity existingItem = item(1L, "19.90", 3);
        cart.getCartItemEntities().add(existingItem);
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));

        assertThrows(CartItemNotFoundException.class, () -> service.updateQuantity(10L, 2L, 5));

        assertThat(existingItem.getQuantity()).isEqualTo(3);
    }

    @Test
    void shouldThrowWhenUpdatingQuantityInMissingCart() {
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> service.updateQuantity(10L, 1L, 5));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void shouldRejectInvalidQuantity(Integer quantity) {
        assertThrows(IllegalArgumentException.class, () -> service.updateQuantity(10L, 1L, quantity));

        verifyNoInteractions(cartRepository, productRepository, productCardsRepository, productService);
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
