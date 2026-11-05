package ru.nsu.marketplace.order;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.cart.CartEntity;
import ru.nsu.marketplace.cart.CartItemEntity;
import ru.nsu.marketplace.cart.CartRepository;
import ru.nsu.marketplace.cart.CartService;
import ru.nsu.marketplace.cart.exception.CartNotFoundException;
import ru.nsu.marketplace.order.dto.OrderItemResponse;
import ru.nsu.marketplace.order.dto.OrderResponse;
import ru.nsu.marketplace.order.exception.OrderNotFoundException;
import ru.nsu.marketplace.order.exception.OrderStateException;
import ru.nsu.marketplace.product.model.ProductEntity;
import ru.nsu.marketplace.product.stock.ProductStockEntity;
import ru.nsu.marketplace.product.stock.ProductStockService;
import ru.nsu.marketplace.user.UserRepository;
import ru.nsu.marketplace.user.exception.UserNotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final ProductStockService stockService;

    @Transactional
    public OrderResponse createOrder(Long userId) {
        CartEntity cart = cartRepository.findByUserIdForUpdate(userId).orElseThrow(
                () -> new CartNotFoundException("User cart is missing: userId=" + userId)
        );

        if (cart.getItems().isEmpty()) {
            throw new OrderStateException("Cannot create an order from an empty cart");
        }

        deductStockForCart(cart);

        OrderEntity order = OrderEntity.builder().user(cart.getUser()).build();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItemEntity cartItem : cart.getItems()) {
            OrderItemEntity orderItem = toOrderItem(cartItem, order);
            order.getItems().add(orderItem);
            BigDecimal subtotal = orderItem.getUnitPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
            totalPrice = totalPrice.add(subtotal);
        }

        order.setTotalPrice(totalPrice);
        OrderEntity savedOrder = orderRepository.saveAndFlush(order);
        cart.getItems().clear();

        return toResponse(savedOrder);
    }

    private void deductStockForCart(CartEntity cart) {
        List<CartItemEntity> itemsSortedByProductId = cart.getItems().stream()
            .sorted(Comparator.comparingLong(item -> item.getProduct().getId()))
            .toList();

        for (CartItemEntity cartItem : itemsSortedByProductId) {
            ProductEntity product = cartItem.getProduct();
            ProductStockEntity stockEntity = stockService.getProductStockEntityForUpdate(product.getId());
            int requestedQuantity = cartItem.getQuantity();
            int availableQuantity = stockEntity.getQuantity();

            if (requestedQuantity > availableQuantity) {
                throw new OrderStateException(
                        "Insufficient stock for product: productId=" + product.getPublicId()
                                + ", requestedQuantity=" + requestedQuantity
                                + ", availableQuantity=" + availableQuantity
                );
            }

            stockEntity.setQuantity(availableQuantity - requestedQuantity);
        }
    }

    private OrderItemEntity toOrderItem(CartItemEntity cartItem, OrderEntity order) {
        ProductEntity product = cartItem.getProduct();
        return OrderItemEntity.builder()
                .order(order)
                .product(product)
                .quantity(cartItem.getQuantity())
                .unitPrice(product.getPrice())
                .build();
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
        OrderEntity order = findUserOrderForUpdate(userId, orderId);
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
        OrderEntity order = orderRepository.findById(orderId).orElseThrow(
                () -> new OrderNotFoundException("Order not found: orderId=" + orderId)
        );
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrderByAdmin(Long orderId) {
        OrderEntity order = findOrderForUpdate(orderId);
        return cancelOrder(order);
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

    private OrderEntity findUserOrderForUpdate(Long userId, Long orderId) {
        return orderRepository.findUserOrderForUpdate(orderId, userId).orElseThrow(
                () -> new OrderNotFoundException("Order not found: orderId=" + orderId)
        );
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

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderStateException("An already cancelled order cannot be cancelled again");
        }

        cartService.restoreUserCartFromItems(order.getItems(), order.getUser().getId());
        restoreStockForOrder(order);

        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(order);
    }

    private void restoreStockForOrder(OrderEntity order) {
        List<OrderItemEntity> itemsSortedByProductId = order.getItems().stream()
                .sorted(Comparator.comparingLong(item -> item.getProduct().getId()))
                .toList();

        for (OrderItemEntity orderItem : itemsSortedByProductId) {
            ProductEntity product = orderItem.getProduct();
            ProductStockEntity stock = stockService.getProductStockEntityForUpdate(product.getId());
            long quantity = (long) stock.getQuantity() + orderItem.getQuantity();

            if (quantity > Integer.MAX_VALUE) {
                throw new OrderStateException("Stock return exceeds maximum quantity: productId="
                        + product.getPublicId());
            }

            stock.setQuantity((int) quantity);
        }
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
