package ru.nsu.marketplace.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.nsu.marketplace.dto.OrderResponse;
import ru.nsu.marketplace.entity.*;
import ru.nsu.marketplace.enums.OrderStatus;
import ru.nsu.marketplace.exceptions.*;
import ru.nsu.marketplace.repository.CartRepository;
import ru.nsu.marketplace.repository.OrderRepository;
import ru.nsu.marketplace.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService service;

    @Test
    void shouldCreateOrderWithFixedPricesAndClearCart() {
        CartEntity cart = cart();
        ProductEntity product = cart.getCartItemEntities().getFirst().getProduct();
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));
        when(orderRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        OrderResponse response = service.createOrder(10L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(OrderStatus.IN_DELIVERY);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.deliveredAt()).isNull();
        assertThat(response.totalPrice()).isEqualByComparingTo("49.80");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().getFirst().unitPrice()).isEqualByComparingTo("19.90");
        assertThat(response.items().getFirst().quantity()).isEqualTo(2);
        assertThat(cart.getCartItemEntities()).isEmpty();

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).saveAndFlush(captor.capture());
        OrderEntity order = captor.getValue();
        assertThat(order.getItems()).allSatisfy(item -> assertThat(item.getOrder()).isSameAs(order));
        product.setPrice(new BigDecimal("100.00"));
        assertThat(order.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("19.90");
        assertThat(order.getTotalPrice()).isEqualByComparingTo("49.80");
        verify(cartRepository, never()).delete(any());
    }

    @Test
    void shouldRejectCheckoutFromEmptyCart() {
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(new CartEntity()));

        assertThrows(OrderStateException.class, () -> service.createOrder(10L));

        verifyNoInteractions(orderRepository);
    }

    @Test
    void shouldRejectCheckoutWhenCartIsMissing() {
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> service.createOrder(10L));
        verifyNoInteractions(orderRepository);
    }

    @Test
    void shouldNotClearCartWhenOrderCannotBeSaved() {
        CartEntity cart = cart();
        when(cartRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(cart));
        when(orderRepository.saveAndFlush(any())).thenThrow(new IllegalStateException("Database failure"));

        assertThrows(IllegalStateException.class, () -> service.createOrder(10L));

        assertThat(cart.getCartItemEntities()).hasSize(2);
    }

    @Test
    void shouldReadOrderUsingSavedPriceRatherThanCurrentProductPrice() {
        OrderEntity order = order(OrderStatus.IN_DELIVERY);
        OrderItemEntity item = OrderItemEntity.builder()
                .order(order)
                .product(ProductEntity.builder().publicId(UUID.randomUUID()).price(new BigDecimal("100.00")).build())
                .quantity(2)
                .unitPrice(new BigDecimal("19.90"))
                .build();
        order.getItems().add(item);
        order.setTotalPrice(new BigDecimal("39.80"));
        when(orderRepository.findByIdAndUser_Id(1L, 10L)).thenReturn(Optional.of(order));

        OrderResponse response = service.getUserOrder(10L, 1L);

        assertThat(response.items().getFirst().unitPrice()).isEqualByComparingTo("19.90");
        assertThat(response.items().getFirst().subtotal()).isEqualByComparingTo("39.80");
        assertThat(response.totalPrice()).isEqualByComparingTo("39.80");
    }

    @Test
    void shouldRejectAccessToAnotherUserOrder() {
        when(orderRepository.findByIdAndUser_Id(1L, 20L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> service.getUserOrder(20L, 1L));
        verify(orderRepository, never()).findById(any());
    }

    @Test
    void shouldCancelOwnOrderWithoutReturningItemsToCart() {
        OrderEntity order = order(OrderStatus.IN_DELIVERY);
        when(orderRepository.findUserOrderForUpdate(1L, 10L)).thenReturn(Optional.of(order));

        OrderResponse response = service.cancelUserOrder(10L, 1L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.deliveredAt()).isNull();
        verifyNoInteractions(cartRepository);
    }

    @Test
    void shouldRejectCancellationOfAnotherUserOrder() {
        when(orderRepository.findUserOrderForUpdate(1L, 20L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> service.cancelUserOrder(20L, 1L));
    }

    @Test
    void shouldRejectCancellationOfDeliveredOrder() {
        OrderEntity order = order(OrderStatus.DELIVERED);
        when(orderRepository.findUserOrderForUpdate(1L, 10L)).thenReturn(Optional.of(order));

        assertThrows(OrderStateException.class, () -> service.cancelUserOrder(10L, 1L));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void shouldAllowRepeatedCancellation() {
        when(orderRepository.findUserOrderForUpdate(1L, 10L)).thenReturn(Optional.of(order(OrderStatus.CANCELLED)));

        assertThat(service.cancelUserOrder(10L, 1L).status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldAllowAdminToCancelOrder() {
        when(orderRepository.findOrderForUpdate(1L)).thenReturn(Optional.of(order(OrderStatus.IN_DELIVERY)));

        assertThat(service.cancelOrderByAdmin(1L).status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldRejectAdminCancellationOfDeliveredOrder() {
        when(orderRepository.findOrderForUpdate(1L)).thenReturn(Optional.of(order(OrderStatus.DELIVERED)));

        assertThrows(OrderStateException.class, () -> service.cancelOrderByAdmin(1L));
    }

    @Test
    void shouldRecordDeliveryTimeAndPreserveItOnRepeatedDelivery() {
        OrderEntity order = order(OrderStatus.IN_DELIVERY);
        when(orderRepository.findOrderForUpdate(1L)).thenReturn(Optional.of(order));

        OrderResponse response = service.markOrderDelivered(1L);
        Instant deliveredAt = response.deliveredAt();

        assertThat(response.status()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(deliveredAt).isNotNull();
        assertThat(service.markOrderDelivered(1L).deliveredAt()).isEqualTo(deliveredAt);
    }

    @Test
    void shouldRejectDeliveryOfCancelledOrder() {
        OrderEntity order = order(OrderStatus.CANCELLED);
        when(orderRepository.findOrderForUpdate(1L)).thenReturn(Optional.of(order));

        assertThrows(OrderStateException.class, () -> service.markOrderDelivered(1L));
        assertThat(order.getDeliveredAt()).isNull();
    }

    @Test
    void shouldReturnOnlyCurrentOrders() {
        Pageable pageable = PageRequest.of(0, 20);
        when(orderRepository.findAllByUser_IdAndStatusIn(10L, List.of(OrderStatus.IN_DELIVERY), pageable))
                .thenReturn(new PageImpl<>(List.of(order(OrderStatus.IN_DELIVERY))));

        assertThat(service.getCurrentOrders(10L, pageable).getContent()).hasSize(1);
    }

    @Test
    void shouldReturnDeliveredAndCancelledOrdersInHistory() {
        Pageable pageable = PageRequest.of(0, 20);
        when(orderRepository.findAllByUser_IdAndStatusIn(10L,
                List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED), pageable))
                .thenReturn(new PageImpl<>(List.of(order(OrderStatus.DELIVERED), order(OrderStatus.CANCELLED))));

        assertThat(service.getOrderHistory(10L, pageable).getContent()).extracting(OrderResponse::status)
                .containsExactly(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
    }

    @Test
    void shouldAllowAdminToReadUserOrders() {
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.existsById(10L)).thenReturn(true);
        when(orderRepository.findAllByUser_Id(10L, pageable)).thenReturn(new PageImpl<>(List.of(order(OrderStatus.IN_DELIVERY))));

        assertThat(service.getOrdersByUser(10L, pageable)).hasSize(1);
    }

    @Test
    void shouldRejectAdminRequestForMissingUser() {
        when(userRepository.existsById(10L)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> service.getOrdersByUser(10L, PageRequest.of(0, 20)));
        verifyNoInteractions(orderRepository);
    }

    @Test
    void shouldReturnNotFoundForMissingOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        when(orderRepository.findOrderForUpdate(1L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> service.getOrder(1L));
        assertThrows(OrderNotFoundException.class, () -> service.markOrderDelivered(1L));
    }

    private CartEntity cart() {
        CartEntity cart = new CartEntity();
        cart.setUser(UserEntity.builder().id(10L).build());
        cart.getCartItemEntities().add(cartItem(cart, "19.90", 2));
        cart.getCartItemEntities().add(cartItem(cart, "5.00", 2));
        return cart;
    }

    private CartItemEntity cartItem(CartEntity cart, String price, int quantity) {
        CartItemEntity item = new CartItemEntity();
        item.setCart(cart);
        item.setProduct(ProductEntity.builder().publicId(UUID.randomUUID()).price(new BigDecimal(price)).build());
        item.setQuantity(quantity);
        return item;
    }

    private OrderEntity order(OrderStatus status) {
        return OrderEntity.builder().id(1L).user(UserEntity.builder().id(10L).build())
                .status(status).totalPrice(BigDecimal.ZERO).build();
    }
}
