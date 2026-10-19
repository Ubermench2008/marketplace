package ru.nsu.marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.nsu.marketplace.dto.OrderResponse;
import ru.nsu.marketplace.entity.*;
import ru.nsu.marketplace.enums.OrderStatus;
import ru.nsu.marketplace.enums.Role;
import ru.nsu.marketplace.repository.CartRepository;
import ru.nsu.marketplace.repository.OrderRepository;
import ru.nsu.marketplace.repository.ProductRepository;
import ru.nsu.marketplace.repository.UserRepository;
import ru.nsu.marketplace.service.OrderService;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import ru.nsu.marketplace.exceptions.OrderStateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.jwt.secretKey=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "app.initial-admin.name=test_admin",
        "app.initial-admin.telephone=+79991234567",
        "app.initial-admin.password=AdminPassword123",
        "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@Testcontainers
public class OrderIntegrationTest {
    @Container
    @ServiceConnection
    private static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    private static final AtomicLong telephoneSuffix = new AtomicLong(1234570L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @MockitoSpyBean
    private OrderRepository orderRepository;

    @Test
    void shouldCheckoutAndKeepHistoricalPriceAfterProductPriceChanges() throws Exception {
        CartEntity cart = cart();
        Long userId = cart.getUser().getId();
        Long productId = cart.getCartItemEntities().getFirst().getProduct().getId();

        OrderResponse order = createOrder(userId);

        assertThat(order.status()).isEqualTo(OrderStatus.IN_DELIVERY);
        assertThat(order.createdAt()).isNotNull();
        assertThat(order.deliveredAt()).isNull();
        assertThat(order.totalPrice()).isEqualByComparingTo("39.80");
        assertThat(order.items()).hasSize(1);
        assertThat(order.items().getFirst().quantity()).isEqualTo(2);
        assertThat(order.items().getFirst().unitPrice()).isEqualByComparingTo("19.90");
        assertThat(itemsCount(cart.getId())).isZero();
        assertThat(cartRepository.findById(cart.getId())).isPresent();
        assertThat(jdbcTemplate.queryForObject("select count(*) from order_items where order_id = ?",
                Long.class, order.id())).isEqualTo(1L);

        ProductEntity product = productRepository.findById(productId).orElseThrow();
        product.setPrice(new BigDecimal("99.00"));
        productRepository.saveAndFlush(product);

        String json = mockMvc.perform(get("/api/orders/{orderId}", order.id())
                        .with(authenticated(userId, Role.BUYER)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        OrderResponse savedOrder = mapper.readValue(json, OrderResponse.class);
        assertThat(savedOrder.items().getFirst().unitPrice()).isEqualByComparingTo("19.90");
        assertThat(savedOrder.totalPrice()).isEqualByComparingTo("39.80");

        mockMvc.perform(post("/api/orders").with(authenticated(userId, Role.BUYER)))
                .andExpect(status().isConflict());
        assertThat(jdbcTemplate.queryForObject("select count(*) from orders where user_id = ?",
                Long.class, userId)).isEqualTo(1L);
    }

    @Test
    void shouldMoveCancelledOrderToHistoryAndProtectItFromOtherUsers() throws Exception {
        CartEntity cart = cart();
        Long userId = cart.getUser().getId();
        Long otherUserId = user(Role.BUYER).getId();
        OrderResponse order = createOrder(userId);

        mockMvc.perform(get("/api/orders/{orderId}", order.id()).with(authenticated(otherUserId, Role.BUYER)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/orders/{orderId}/cancel", order.id()).with(authenticated(otherUserId, Role.BUYER)))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/orders/{orderId}/cancel", order.id()).with(authenticated(userId, Role.BUYER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(get("/api/orders/current").with(authenticated(userId, Role.BUYER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        mockMvc.perform(get("/api/orders/history").with(authenticated(userId, Role.BUYER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(order.id()))
                .andExpect(jsonPath("$.content[0].status").value("CANCELLED"));
        assertThat(itemsCount(cart.getId())).isZero();
    }

    @Test
    void shouldAllowAdminCancellationAndDeliveryButRejectCancellationAfterDelivery() throws Exception {
        CartEntity firstCart = cart();
        OrderResponse firstOrder = createOrder(firstCart.getUser().getId());
        Long adminId = userRepository.findByTelephoneNumber("+79991234567").orElseThrow().getId();

        mockMvc.perform(get("/api/admin/users/{userId}/orders", firstCart.getUser().getId())
                        .with(authenticated(adminId, Role.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(firstOrder.id()));
        mockMvc.perform(patch("/api/admin/orders/{orderId}/cancel", firstOrder.id())
                        .with(authenticated(adminId, Role.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(patch("/api/admin/orders/{orderId}/deliver", firstOrder.id())
                        .with(authenticated(adminId, Role.ADMIN)))
                .andExpect(status().isConflict());

        CartEntity secondCart = cart();
        Long userId = secondCart.getUser().getId();
        OrderResponse secondOrder = createOrder(userId);
        String json = mockMvc.perform(patch("/api/admin/orders/{orderId}/deliver", secondOrder.id())
                        .with(authenticated(adminId, Role.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERED"))
                .andReturn().getResponse().getContentAsString();
        Instant deliveredAt = mapper.readValue(json, OrderResponse.class).deliveredAt();
        assertThat(deliveredAt).isNotNull();
        assertThat(orderRepository.findById(secondOrder.id()).orElseThrow().getDeliveredAt())
                .isBetween(deliveredAt.minusNanos(1000), deliveredAt.plusNanos(1000));

        mockMvc.perform(patch("/api/orders/{orderId}/cancel", secondOrder.id()).with(authenticated(userId, Role.BUYER)))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/admin/orders/{orderId}/cancel", secondOrder.id()).with(authenticated(adminId, Role.ADMIN)))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/orders/current").with(authenticated(userId, Role.BUYER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        mockMvc.perform(get("/api/orders/history").with(authenticated(userId, Role.BUYER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].status").value("DELIVERED"));
    }

    @Test
    void shouldRollbackPersistedOrderAndKeepCartWhenCheckoutFails() {
        CartEntity cart = cart();
        Long userId = cart.getUser().getId();
        doAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            // Execute real persistence before failing the enclosing service transaction.
            jdbcTemplate.update("insert into orders (id, user_id, created_at, total_price, status) values (?, ?, ?, ?, ?)",
                    -userId, userId, Timestamp.from(order.getCreatedAt()), order.getTotalPrice(), order.getStatus().name());
            throw new DataAccessResourceFailureException("Test checkout failure");
        }).when(orderRepository).saveAndFlush(any(OrderEntity.class));

        assertThrows(DataAccessResourceFailureException.class, () -> orderService.createOrder(userId));

        assertThat(jdbcTemplate.queryForObject("select count(*) from orders where user_id = ?",
                Long.class, userId)).isZero();
        assertThat(itemsCount(cart.getId())).isEqualTo(1L);
    }

    @Test
    void shouldCreateOnlyOneOrderWhenCheckoutRequestsRunConcurrently() throws Exception {
        CartEntity cart = cart();
        Long userId = cart.getUser().getId();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> checkout = () -> {
                start.await();
                try {
                    orderService.createOrder(userId);
                    return true;
                } catch (OrderStateException exception) {
                    return false;
                }
            };
            Future<Boolean> first = executor.submit(checkout);
            Future<Boolean> second = executor.submit(checkout);
            start.countDown();

            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }

        assertThat(jdbcTemplate.queryForObject("select count(*) from orders where user_id = ?",
                Long.class, userId)).isEqualTo(1L);
        assertThat(itemsCount(cart.getId())).isZero();
    }

    private OrderResponse createOrder(Long userId) throws Exception {
        String json = mockMvc.perform(post("/api/orders").with(authenticated(userId, Role.BUYER)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readValue(json, OrderResponse.class);
    }

    private CartEntity cart() {
        UserEntity buyer = user(Role.BUYER);
        UserEntity seller = user(Role.SELLER);
        ProductEntity product = productRepository.save(ProductEntity.builder()
                .publicId(UUID.randomUUID())
                .slug(UUID.randomUUID().toString())
                .name("Order test product")
                .description("A sufficiently long product description for tests.")
                .price(new BigDecimal("19.90"))
                .seller(seller)
                .build());
        CartEntity cart = new CartEntity();
        cart.setUser(buyer);
        CartItemEntity item = new CartItemEntity();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(2);
        cart.getCartItemEntities().add(item);
        return cartRepository.saveAndFlush(cart);
    }

    private UserEntity user(Role role) {
        return userRepository.save(UserEntity.builder()
                .name("order_test_user")
                .telephoneNumber("+7999" + telephoneSuffix.getAndIncrement())
                .passwordHash("test-password-hash")
                .roles(Set.of(role))
                .build());
    }

    private Long itemsCount(Long cartId) {
        return jdbcTemplate.queryForObject("select count(*) from cart_items where cart_id = ?", Long.class, cartId);
    }

    private RequestPostProcessor authenticated(Long userId, Role role) {
        return authentication(new UsernamePasswordAuthenticationToken(userId, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
}
