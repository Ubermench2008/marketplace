package ru.nsu.marketplace.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import ru.nsu.marketplace.auth.AuthController;
import ru.nsu.marketplace.cart.AdminCartController;
import ru.nsu.marketplace.cart.CartController;
import ru.nsu.marketplace.common.web.GlobalExceptionHandler;
import ru.nsu.marketplace.order.AdminOrderController;
import ru.nsu.marketplace.order.OrderController;
import ru.nsu.marketplace.product.api.CatalogController;
import ru.nsu.marketplace.product.api.ProductController;
import ru.nsu.marketplace.product.api.SellerController;
import ru.nsu.marketplace.product.moderation.ProductModerationController;
import ru.nsu.marketplace.review.ProductReviewController;
import ru.nsu.marketplace.review.ProductReviewService;
import ru.nsu.marketplace.review.moderation.ProductReviewAndReplyModerationController;
import ru.nsu.marketplace.review.reaction.ProductReviewReactionController;
import ru.nsu.marketplace.review.reply.reaction.ProductReviewReplyReactionController;
import ru.nsu.marketplace.review.reply.dto.CreateProductReviewReplyRequest;
import ru.nsu.marketplace.user.AdminUserController;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiMappingContractTest {
    private MockMvc mockMvc;
    private ProductReviewService reviewService;

    @BeforeEach
    void setUp() throws Exception {
        Class<?>[] types = {AuthController.class, CartController.class, AdminCartController.class,
                OrderController.class, AdminOrderController.class, CatalogController.class,
                ProductController.class, SellerController.class, ProductModerationController.class,
                ProductReviewController.class, ProductReviewAndReplyModerationController.class,
                ProductReviewReactionController.class, ProductReviewReplyReactionController.class,
                AdminUserController.class};
        Object[] controllers = new Object[types.length];
        for (int index = 0; index < types.length; index++) {
            java.lang.reflect.Constructor<?> constructor = types[index].getConstructors()[0];
            Object[] dependencies = Arrays.stream(constructor.getParameterTypes()).map(type -> mock(type)).toArray();
            controllers[index] = constructor.newInstance(dependencies);
            for (Object dependency : dependencies) {
                if (dependency instanceof ProductReviewService service) reviewService = service;
            }
        }
        mockMvc = MockMvcBuilders.standaloneSetup(controllers)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(10L, null, List.of()));
    }

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void shouldRegisterAllControllersWithoutConflictingMappings() {
        RequestMappingHandlerMapping mapping = mockMvc.getDispatcherServlet().getWebApplicationContext()
                .getBean(RequestMappingHandlerMapping.class);
        Set<String> routes = mapping.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getMethodsCondition().getMethods().stream()
                        .flatMap(method -> info.getPatternValues().stream().map(path -> method + " " + path)))
                .collect(Collectors.toSet());
        assertThat(routes).contains(
                "GET /api/admin/users/{userId}/cart", "POST /api/cart/items",
                "GET /api/moderation/product-requests", "GET /api/moderation/review-requests",
                "PATCH /api/moderation/review-replies/{reviewReplyId}",
                "POST /api/reviews/{reviewId}/replies", "PUT /api/reviews/{reviewId}/reaction",
                "PUT /api/review-replies/{replyId}/reaction", "POST /api/orders/{orderId}/cancel",
                "POST /api/admin/orders/{orderId}/deliver", "POST /api/products/{productId}/archive");
        assertThat(routes).doesNotContain("POST /api/cart/{productId}", "GET /api/cart/{userId}",
                "POST /api/reviews/{reviewId}/reply", "GET /api/moderation/review",
                "GET /api/moderation/requests", "PATCH /api/orders/{orderId}/cancel");
    }

    @Test
    void shouldCreateReplyWith201UsingRequestDto() throws Exception {
        mockMvc.perform(post("/api/reviews/5/replies").contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Ответ участнику\",\"replyToId\":7}"))
                .andExpect(status().isCreated()).andExpect(content().string(""));
        verify(reviewService).createProductReviewReply(5L, new CreateProductReviewReplyRequest("Ответ участнику", 7L), 10L);
    }

    @Test
    void shouldSetAndRemoveBothKindsOfReaction() throws Exception {
        for (String path : List.of("/api/reviews/5/reaction", "/api/review-replies/7/reaction")) {
            mockMvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"LIKE\"}"))
                    .andExpect(status().isNoContent());
            mockMvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"DISLIKE\"}"))
                    .andExpect(status().isNoContent());
            mockMvc.perform(delete(path)).andExpect(status().isNoContent());
            mockMvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"OTHER\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void shouldRejectRemovedPathsAndWrongMethodsWithout500() throws Exception {
        mockMvc.perform(post("/api/reviews/5/reaction").contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"LIKE\"}")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get("/api/moderation/requests")).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/reviews/not-a-number/reaction").contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"LIKE\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldValidateCartItemRequest() throws Exception {
        mockMvc.perform(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"00000000-0000-0000-0000-000000000005\"}"))
                .andExpect(status().isNoContent());
    }
}
