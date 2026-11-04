package ru.nsu.marketplace.order;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.cart.CartEntity;
import ru.nsu.marketplace.cart.CartItemEntity;
import ru.nsu.marketplace.cart.CartRepository;
import ru.nsu.marketplace.cart.exception.CartNotFoundException;
import ru.nsu.marketplace.order.dto.OrderItemResponse;
import ru.nsu.marketplace.order.dto.OrderResponse;
import ru.nsu.marketplace.order.exception.OrderNotFoundException;
import ru.nsu.marketplace.order.exception.OrderStateException;
import ru.nsu.marketplace.product.model.ProductEntity;
import ru.nsu.marketplace.user.UserRepository;
import ru.nsu.marketplace.user.exception.UserNotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(Long userId) {
        CartEntity cart = cartRepository.findByUserIdForUpdate(userId).orElseThrow(
                () -> new CartNotFoundException("User cart is missing: userId=" + userId)
        );

        if (cart.getItems().isEmpty()) {
            throw new OrderStateException("Cannot create an order from an empty cart");
        }

        OrderEntity order = OrderEntity.builder().user(cart.getUser()).build();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItemEntity cartItem : cart.getItems()) {
            ProductEntity product = cartItem.getProduct();
            BigDecimal unitPrice = product.getPrice();
            int quantity = cartItem.getQuantity();

            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .build();
            order.getItems().add(orderItem);
            totalPrice = totalPrice.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        }

        order.setTotalPrice(totalPrice);
        OrderEntity savedOrder = orderRepository.saveAndFlush(order);
        cart.getItems().clear();

        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getCurrentOrders(Long userId, Pageable pageable) {
        return orderRepository.findAllByUser_IdAndStatusIn(userId, List.of(OrderStatus.IN_DELIVERY), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrderHistory(Long userId, Pageable pageable) {
        return orderRepository.findAllByUser_IdAndStatusIn(userId,
                        List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getUserOrder(Long userId, Long orderId) {
        OrderEntity order = orderRepository.findByIdAndUser_Id(orderId, userId).orElseThrow(
                () -> new OrderNotFoundException("Order not found: orderId=" + orderId)
        );
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelUserOrder(Long userId, Long orderId) {
        OrderEntity order = orderRepository.findUserOrderForUpdate(orderId, userId).orElseThrow(
                () -> new OrderNotFoundException("Order not found: orderId=" + orderId)
        );
        return cancelOrder(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByUser(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found: userId=" + userId);
        }
        return orderRepository.findAllByUser_Id(userId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return toResponse(orderRepository.findById(orderId).orElseThrow(
                () -> new OrderNotFoundException("Order not found: orderId=" + orderId)
        ));
    }

    @Transactional
    public OrderResponse cancelOrderByAdmin(Long orderId) {
        return cancelOrder(findOrderForUpdate(orderId));
    }

    @Transactional
    public OrderResponse markOrderDelivered(Long orderId) {
        OrderEntity order = findOrderForUpdate(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderStateException("A cancelled order cannot be delivered");
        }
        if (order.getStatus() == OrderStatus.IN_DELIVERY) {
            order.setStatus(OrderStatus.DELIVERED);
            order.setDeliveredAt(Instant.now());
        }
        return toResponse(order);
    }

    private OrderEntity findOrderForUpdate(Long orderId) {
        return orderRepository.findOrderForUpdate(orderId).orElseThrow(
                () -> new OrderNotFoundException("Order not found: orderId=" + orderId)
        );
    }

    private OrderResponse cancelOrder(OrderEntity order) {
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderStateException("A delivered order cannot be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(order);
    }

    private OrderResponse toResponse(OrderEntity order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getPublicId(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();

        return new OrderResponse(order.getId(), order.getUser().getId(), order.getStatus(),
                order.getCreatedAt(), order.getDeliveredAt(), order.getTotalPrice(), items);
    }
}
