package ru.nsu.marketplace.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.nsu.marketplace.auth.JwtAuthenticationFilter;
import ru.nsu.marketplace.auth.JwtService;
import ru.nsu.marketplace.auth.SecurityConfig;
import ru.nsu.marketplace.order.dto.OrderResponse;
import ru.nsu.marketplace.order.exception.OrderNotFoundException;
import ru.nsu.marketplace.order.exception.OrderStateException;
import ru.nsu.marketplace.user.Role;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ OrderController.class, AdminOrderController.class })
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService service;

    @MockitoBean
    private JwtService jwtService;

    @ParameterizedTest
    @EnumSource(value = Role.class, names = { "BUYER", "SELLER" })
    void shouldCreateCustomerOrder(Role role) throws Exception {
        when(service.createOrder(10L)).thenReturn(response(OrderStatus.IN_DELIVERY));

        mockMvc
            .perform(post("/api/orders").with(authenticated(role)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("IN_DELIVERY"));

        verify(service).createOrder(10L);
    }

    @Test
    void shouldReadCurrentOrdersWithPagination() throws Exception {
        when(service.getCurrentOrders(eq(10L), any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(response(OrderStatus.IN_DELIVERY)))
        );

        mockMvc
            .perform(
                get("/api/orders").param("view", "CURRENT")
                    .with(authenticated(Role.BUYER))
                    .param("page", "1")
                    .param("size", "5")
            )
            .andExpect(status().isOk());

        verify(service).getCurrentOrders(
            eq(10L),
            argThat(
                page ->
                    page.getPageNumber() == 1 &&
                    page.getPageSize() == 5 &&
                    page.getSort().getOrderFor("createdAt").isDescending()
            )
        );
    }

    @Test
    void shouldReadOrderHistory() throws Exception {
        when(service.getOrderHistory(eq(10L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc
            .perform(get("/api/orders").param("view", "HISTORY").with(authenticated(Role.BUYER)))
            .andExpect(status().isOk());

        verify(service).getOrderHistory(eq(10L), any(Pageable.class));
    }

    @Test
    void shouldReadOnlyOwnOrder() throws Exception {
        when(service.getUserOrder(10L, 1L)).thenReturn(response(OrderStatus.IN_DELIVERY));

        mockMvc.perform(get("/api/orders/1").with(authenticated(Role.BUYER))).andExpect(status().isOk());

        verify(service).getUserOrder(10L, 1L);
    }

    @Test
    void shouldCancelOwnOrder() throws Exception {
        when(service.cancelUserOrder(10L, 1L)).thenReturn(response(OrderStatus.CANCELLED));

        mockMvc
            .perform(post("/api/orders/1/cancel").with(authenticated(Role.BUYER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(service).cancelUserOrder(10L, 1L);
    }

    @Test
    void shouldReturnNotFoundForForeignOrder() throws Exception {
        when(service.getUserOrder(10L, 1L)).thenThrow(new OrderNotFoundException("Order not found"));
        when(service.cancelUserOrder(10L, 1L)).thenThrow(new OrderNotFoundException("Order not found"));

        mockMvc
            .perform(get("/api/orders/1").with(authenticated(Role.BUYER)))
            .andExpect(status().isNotFound());
        mockMvc
            .perform(post("/api/orders/1/cancel").with(authenticated(Role.BUYER)))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictForEmptyCartOrDeliveredOrder() throws Exception {
        when(service.createOrder(10L)).thenThrow(new OrderStateException("Empty cart"));
        when(service.cancelUserOrder(10L, 1L)).thenThrow(new OrderStateException("Already delivered"));

        mockMvc
            .perform(post("/api/orders").with(authenticated(Role.BUYER)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
        mockMvc
            .perform(post("/api/orders/1/cancel").with(authenticated(Role.BUYER)))
            .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = { "create", "current", "history", "details", "cancel" })
    void shouldRejectAnonymousAndAdminCustomerRequests(String operation) throws Exception {
        mockMvc.perform(customerRequest(operation)).andExpect(status().isUnauthorized());
        mockMvc
            .perform(customerRequest(operation).with(authenticated(Role.ADMIN)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void shouldAllowAdminToReadAndManageUserOrders() throws Exception {
        when(service.getOrdersByUser(eq(20L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(service.getOrder(1L)).thenReturn(response(OrderStatus.IN_DELIVERY));
        when(service.cancelOrderByAdmin(1L)).thenReturn(response(OrderStatus.CANCELLED));
        when(service.markOrderDelivered(2L)).thenReturn(response(OrderStatus.DELIVERED));

        mockMvc
            .perform(get("/api/admin/users/20/orders").with(authenticated(Role.ADMIN)))
            .andExpect(status().isOk());
        mockMvc
            .perform(get("/api/admin/orders/1").with(authenticated(Role.ADMIN)))
            .andExpect(status().isOk());
        mockMvc
            .perform(post("/api/admin/orders/1/cancel").with(authenticated(Role.ADMIN)))
            .andExpect(status().isOk());
        mockMvc
            .perform(post("/api/admin/orders/2/deliver").with(authenticated(Role.ADMIN)))
            .andExpect(status().isOk());

        verify(service).getOrdersByUser(eq(20L), any(Pageable.class));
        verify(service).getOrder(1L);
        verify(service).cancelOrderByAdmin(1L);
        verify(service).markOrderDelivered(2L);
    }

    @ParameterizedTest
    @ValueSource(strings = { "list", "details", "cancel", "deliver" })
    void shouldRejectCustomerAndAnonymousAdminRequests(String operation) throws Exception {
        mockMvc.perform(adminRequest(operation)).andExpect(status().isUnauthorized());
        for (Role role : List.of(Role.BUYER, Role.SELLER)) {
            mockMvc
                .perform(adminRequest(operation).with(authenticated(role)))
                .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    private MockHttpServletRequestBuilder customerRequest(String operation) {
        return switch (operation) {
            case "create" -> post("/api/orders");
            case "current" -> get("/api/orders").param("view", "CURRENT");
            case "history" -> get("/api/orders").param("view", "HISTORY");
            case "details" -> get("/api/orders/1");
            case "cancel" -> post("/api/orders/1/cancel");
            default -> throw new IllegalArgumentException(operation);
        };
    }

    private MockHttpServletRequestBuilder adminRequest(String operation) {
        return switch (operation) {
            case "list" -> get("/api/admin/users/20/orders");
            case "details" -> get("/api/admin/orders/1");
            case "cancel" -> post("/api/admin/orders/1/cancel");
            case "deliver" -> post("/api/admin/orders/1/deliver");
            default -> throw new IllegalArgumentException(operation);
        };
    }

    private RequestPostProcessor authenticated(Role role) {
        return authentication(
            new UsernamePasswordAuthenticationToken(
                10L,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            )
        );
    }

    private OrderResponse response(OrderStatus status) {
        return new OrderResponse(1L, 10L, status, Instant.now(), null, BigDecimal.ZERO, List.of());
    }
}
