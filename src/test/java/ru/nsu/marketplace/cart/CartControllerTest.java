package ru.nsu.marketplace.cart;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.nsu.marketplace.auth.JwtAuthenticationFilter;
import ru.nsu.marketplace.auth.JwtService;
import ru.nsu.marketplace.auth.SecurityConfig;
import ru.nsu.marketplace.cart.dto.CartResponse;
import ru.nsu.marketplace.cart.exception.CartItemNotFoundException;
import ru.nsu.marketplace.cart.exception.CartNotFoundException;
import ru.nsu.marketplace.product.exception.ProductNotFoundException;
import ru.nsu.marketplace.user.Role;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ CartController.class, AdminCartController.class })
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService service;

    @MockitoBean
    private JwtService jwtService;

    @ParameterizedTest
    @EnumSource(value = Role.class, names = { "BUYER", "SELLER" })
    void shouldReturnCartOfAuthenticatedCustomer(Role role) throws Exception {
        when(service.getCart(10L)).thenReturn(new CartResponse(List.of(), BigDecimal.ZERO));

        mockMvc
            .perform(get("/api/cart").with(authenticated(10L, role)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isEmpty())
            .andExpect(jsonPath("$.totalPrice").value(0));

        verify(service).getCart(10L);
    }

    @Test
    void shouldRejectAdminCartRequest() throws Exception {
        mockMvc
            .perform(
                get("/api/cart").with(
                    authentication(
                        new UsernamePasswordAuthenticationToken(
                            1L,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        )
                    )
                )
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectUnauthenticatedCartRequest() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void shouldAllowAdminToReadSpecifiedUserCart() throws Exception {
        when(service.getCart(20L)).thenReturn(new CartResponse(List.of(), BigDecimal.ZERO));

        mockMvc
            .perform(get("/api/admin/users/{userId}/cart", 20L).with(authenticated(1L, Role.ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isEmpty())
            .andExpect(jsonPath("$.totalPrice").value(0));

        verify(service).getCart(20L);
        verify(service, never()).getCart(1L);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = { "BUYER", "SELLER" })
    void shouldRejectCustomerRequestToSpecifiedUserCart(Role role) throws Exception {
        mockMvc
            .perform(get("/api/admin/users/{userId}/cart", 20L).with(authenticated(10L, role)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectUnauthenticatedRequestToSpecifiedUserCart() throws Exception {
        mockMvc.perform(get("/api/admin/users/{userId}/cart", 20L)).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnNotFoundWhenAdminRequestsMissingCart() throws Exception {
        when(service.getCart(20L)).thenThrow(new CartNotFoundException("Cart not found"));

        mockMvc
            .perform(get("/api/admin/users/{userId}/cart", 20L).with(authenticated(1L, Role.ADMIN)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Cart not found"));
    }

    @Test
    void shouldReturnNotFoundWhenOwnCartIsMissing() throws Exception {
        when(service.getCart(10L)).thenThrow(new CartNotFoundException("Cart not found"));

        mockMvc
            .perform(get("/api/cart").with(authenticated(10L, Role.BUYER)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = { "BUYER", "SELLER" })
    void shouldAddProductToAuthenticatedCustomerCart(Role role) throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc
            .perform(
                post("/api/cart/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"productId\":\"" + productId + "\"}")
                    .with(authenticated(10L, role))
            )
            .andExpect(status().isNoContent());

        verify(service).addProductToCart(10L, productId);
    }

    @Test
    void shouldRejectAdminProductAddition() throws Exception {
        mockMvc
            .perform(
                post("/api/cart/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"productId\":\"" + UUID.randomUUID() + "\"}")
                    .with(authenticated(1L, Role.ADMIN))
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectUnauthenticatedProductAddition() throws Exception {
        mockMvc
            .perform(
                post("/api/cart/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"productId\":\"" + UUID.randomUUID() + "\"}")
            )
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnNotFoundWhenAddingMissingProduct() throws Exception {
        UUID productId = UUID.randomUUID();
        doThrow(new ProductNotFoundException("Product not found"))
            .when(service)
            .addProductToCart(10L, productId);

        mockMvc
            .perform(
                post("/api/cart/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"productId\":\"" + productId + "\"}")
                    .with(authenticated(10L, Role.BUYER))
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    void shouldReturnNotFoundWhenAddingProductToMissingCart() throws Exception {
        UUID productId = UUID.randomUUID();
        doThrow(new CartNotFoundException("Cart not found")).when(service).addProductToCart(10L, productId);

        mockMvc
            .perform(
                post("/api/cart/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"productId\":\"" + productId + "\"}")
                    .with(authenticated(10L, Role.BUYER))
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Cart not found"));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = { "BUYER", "SELLER" })
    void shouldClearAuthenticatedCustomerCart(Role role) throws Exception {
        mockMvc
            .perform(delete("/api/cart/items").with(authenticated(10L, role)))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        verify(service).clearCart(10L);
    }

    @Test
    void shouldRejectAdminCartClearing() throws Exception {
        mockMvc
            .perform(delete("/api/cart/items").with(authenticated(1L, Role.ADMIN)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectUnauthenticatedCartClearing() throws Exception {
        mockMvc.perform(delete("/api/cart/items")).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnNotFoundWhenClearingMissingCart() throws Exception {
        doThrow(new CartNotFoundException("Cart not found")).when(service).clearCart(10L);

        mockMvc
            .perform(delete("/api/cart/items").with(authenticated(10L, Role.BUYER)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Cart not found"));
    }

    @Test
    void shouldRejectCartMutationsEvenWhenAdminAlsoHasBuyerRole() throws Exception {
        mockMvc
            .perform(
                post("/api/cart/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"productId\":\"" + UUID.randomUUID() + "\"}")
                    .with(authenticated(1L, Role.ADMIN, Role.BUYER))
            )
            .andExpect(status().isForbidden());

        mockMvc
            .perform(delete("/api/cart/items").with(authenticated(1L, Role.ADMIN, Role.BUYER)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = { "BUYER", "SELLER" })
    void shouldDeleteAuthenticatedCustomerCartItem(Role role) throws Exception {
        mockMvc
            .perform(delete("/api/cart/items/{itemId}", 15L).with(authenticated(10L, role)))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        verify(service).deleteCartItem(10L, 15L);
    }

    @Test
    void shouldRejectAdminCartItemDeletion() throws Exception {
        mockMvc
            .perform(delete("/api/cart/items/{itemId}", 15L).with(authenticated(1L, Role.ADMIN, Role.BUYER)))
            .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectUnauthenticatedCartItemDeletion() throws Exception {
        mockMvc.perform(delete("/api/cart/items/{itemId}", 15L)).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnNotFoundWhenCartItemIsMissing() throws Exception {
        doThrow(new CartItemNotFoundException("Cart item not found: itemId=15"))
            .when(service)
            .deleteCartItem(10L, 15L);

        mockMvc
            .perform(delete("/api/cart/items/{itemId}", 15L).with(authenticated(10L, Role.BUYER)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = { "BUYER", "SELLER" })
    void shouldUpdateCustomerCartItemQuantity(Role role) throws Exception {
        mockMvc
            .perform(
                patch("/api/cart/items/{itemId}", 15L)
                    .with(authenticated(10L, role))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quantity\":5}")
            )
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        verify(service).updateCartItemQuantity(10L, 15L, 5);
    }

    @ParameterizedTest
    @ValueSource(strings = { "{}", "{\"quantity\":null}", "{\"quantity\":0}", "{\"quantity\":-1}" })
    void shouldRejectInvalidCartItemQuantity(String body) throws Exception {
        mockMvc
            .perform(
                patch("/api/cart/items/{itemId}", 15L)
                    .with(authenticated(10L, Role.BUYER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectAdminCartItemQuantityUpdate() throws Exception {
        mockMvc
            .perform(
                patch("/api/cart/items/{itemId}", 15L)
                    .with(authenticated(1L, Role.ADMIN, Role.BUYER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quantity\":5}")
            )
            .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void shouldRejectUnauthenticatedCartItemQuantityUpdate() throws Exception {
        mockMvc
            .perform(
                patch("/api/cart/items/{itemId}", 15L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quantity\":5}")
            )
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingCartItem() throws Exception {
        doThrow(new CartItemNotFoundException("Cart item not found: itemId=15"))
            .when(service)
            .updateCartItemQuantity(10L, 15L, 5);

        mockMvc
            .perform(
                patch("/api/cart/items/{itemId}", 15L)
                    .with(authenticated(10L, Role.BUYER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quantity\":5}")
            )
            .andExpect(status().isNotFound());
    }

    private RequestPostProcessor authenticated(Long userId, Role... roles) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
            .toList();

        return authentication(new UsernamePasswordAuthenticationToken(userId, null, authorities));
    }
}
