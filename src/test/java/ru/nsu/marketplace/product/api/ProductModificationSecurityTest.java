package ru.nsu.marketplace.product.api;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import ru.nsu.marketplace.auth.JwtAuthenticationFilter;
import ru.nsu.marketplace.auth.JwtService;
import ru.nsu.marketplace.auth.SecurityConfig;
import ru.nsu.marketplace.product.ProductService;
import ru.nsu.marketplace.product.stock.ProductStockService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class ProductModificationSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService service;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ProductStockService productStockService;

    @ParameterizedTest
    @ValueSource(strings = {"create", "fields", "addProductDetailsImages", "positions", "deleteImage", "preview", "archive"})
    void shouldRejectBuyerAndAdminWithoutSellerRole(String operation) throws Exception {
        UUID productId = UUID.randomUUID();
        for (String role : List.of("BUYER", "ADMIN")) {
            mockMvc.perform(request(operation, productId).with(authentication(
                            new UsernamePasswordAuthenticationToken(20L, null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role)))
                    )))
                    .andExpect(status().isForbidden());
        }

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"create", "fields", "addProductDetailsImages", "positions", "deleteImage", "preview", "archive"})
    void shouldRejectUnauthenticatedModification(String operation) throws Exception {
        mockMvc.perform(request(operation, UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"fields", "addProductDetailsImages", "positions", "deleteImage", "preview", "archive"})
    void shouldReturnForbiddenWhenServiceRejectsNonOwner(String operation) throws Exception {
        UUID productId = UUID.randomUUID();
        AccessDeniedException exception = new AccessDeniedException("Only seller-owner can modify product");

        switch (operation) {
            case "fields" -> doThrow(exception).when(service).updateProduct(eq(productId), eq(20L), any());
            case "addProductDetailsImages" -> doThrow(exception).when(service).addProductDetailsImages(eq(productId), anyList(), eq(20L));
            case "positions" -> doThrow(exception).when(service).updateProductImageOrder(eq(productId), any(), eq(20L));
            case "deleteImage" -> doThrow(exception).when(service).deleteProductDetailsImage(any(), eq(productId), eq(20L));
            case "preview" -> doThrow(exception).when(service).updateProductPreview(eq(productId), any(), eq(20L));
            case "archive" -> doThrow(exception).when(service).archiveProduct(productId, 20L);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        }

        mockMvc.perform(request(operation, productId).with(authentication(
                        new UsernamePasswordAuthenticationToken(20L, null,
                                List.of(new SimpleGrantedAuthority("ROLE_SELLER")))
                )))
                .andExpect(status().isForbidden());
    }

    private AbstractMockHttpServletRequestBuilder<?> request(String operation, UUID productId) {
        return switch (operation) {
            case "create" -> multipart("/api/products")
                    .file(new MockMultipartFile("product", "", MediaType.APPLICATION_JSON_VALUE,
                            """
                            {
                              "slug": "iphone-16-pro",
                              "name": "iphone 16 pro",
                              "description": "A sufficiently long product description for tests.",
                              "price": 99990.00,
                              "initialQuantity": 0,
                              "categorySlug": "electronics"
                            }
                            """.getBytes()))
                    .file(image("previewImage"))
                    .file(image("detailsImages"));
            case "fields" -> patch("/api/products/{id}", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"New name\"}");
            case "addProductDetailsImages" -> multipart("/api/products/{id}/details-images", productId)
                    .file(image("images"));
            case "positions" -> put("/api/products/{id}/details-images/order", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"imageIds\":[\"" + UUID.randomUUID() + "\"]}");
            case "deleteImage" -> delete("/api/products/{id}/details-images/{imageId}", productId, UUID.randomUUID());
            case "preview" -> multipart(HttpMethod.PUT, "/api/products/{id}/preview-image", productId)
                    .file(image("previewImage"));
            case "archive" -> post("/api/products/{id}/archive", productId);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

    private MockMultipartFile image(String partName) {
        return new MockMultipartFile(partName, "image.webp", "image/webp", "image".getBytes());
    }
}
