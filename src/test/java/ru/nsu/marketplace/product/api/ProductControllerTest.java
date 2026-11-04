package ru.nsu.marketplace.product.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.nsu.marketplace.auth.JwtAuthenticationFilter;
import ru.nsu.marketplace.auth.JwtService;
import ru.nsu.marketplace.auth.SecurityConfig;
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.product.ProductService;
import ru.nsu.marketplace.product.dto.CreateProductRequest;
import ru.nsu.marketplace.product.dto.ProductCardResponse;
import ru.nsu.marketplace.product.dto.ProductDetailsImageResponse;
import ru.nsu.marketplace.product.dto.ProductDetailsResponse;
import ru.nsu.marketplace.product.dto.UpdateProductImageOrderRequest;
import ru.nsu.marketplace.product.dto.UpdateProductRequest;
import ru.nsu.marketplace.product.exception.ProductNotFoundException;
import ru.nsu.marketplace.product.model.ProductStatus;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ ProductController.class, CatalogController.class })
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService service;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .defaultRequest(
                get("/").with(
                    authentication(
                        new UsernamePasswordAuthenticationToken(
                            10L,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_SELLER"))
                        )
                    )
                )
            )
            .build();
    }

    @Test
    void shouldCreateProduct() throws Exception {
        ProductDetailsResponse response = new ProductDetailsResponse(
            UUID.randomUUID(),
            "iphone 16 pro",
            "iphone-16-pro",
            new BigDecimal("99990.00"),
            "A sufficiently long product description for tests.",
            List.of(),
            null,
            ProductStatus.OFF,
            ModerationStatus.NOT_SUBMITTED
        );

        when(service.createProduct(any(CreateProductRequest.class), any(), anyList(), eq(10L))).thenReturn(
            response
        );

        mockMvc
            .perform(
                createProductRequest(
                    """
                        {
                          "slug": "iphone-16-pro",
                          "name": "iphone 16 pro",
                          "description": "A sufficiently long product description for tests.",
                          "price": 99990.00
                        }
                        """
                )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("iphone-16-pro"))
            .andExpect(jsonPath("$.name").value("iphone 16 pro"))
            .andExpect(jsonPath("$.price").value(99990.00));
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        mockMvc
            .perform(
                createProductRequest(
                    """
                            {
                              "slug": "iphone-16-pro",
                              "name": "",
                              "description": "A sufficiently long product description for tests.",
                              "price": 99990.00
                            }
                            """
                )
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnBadRequestWhenPriceIsNegative() throws Exception {
        mockMvc
            .perform(
                createProductRequest(
                    """
                            {
                              "slug": "iphone-16-pro",
                              "name": "iphone 16 pro",
                              "description": "A sufficiently long product description for tests.",
                              "price": -99990.00
                            }
                            """
                )
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnBadRequestWhenSlugIsNotExist() throws Exception {
        mockMvc
            .perform(
                createProductRequest(
                    """
                            {
                              "name": "iphone 16 pro",
                              "description": "A sufficiently long product description for tests.",
                              "price": -99990.00
                            }
                            """
                )
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnProductById() throws Exception {
        UUID id = UUID.randomUUID();
        ProductDetailsResponse response = new ProductDetailsResponse(
            id,
            "iphone 16 pro",
            "iphone-16-pro",
            new BigDecimal("99990.00"),
            "A sufficiently long product description for tests.",
            List.of(),
            null,
            ProductStatus.OFF,
            ModerationStatus.NOT_SUBMITTED
        );

        when(service.getProduct(id)).thenReturn(response);

        mockMvc
            .perform(get("/api/products/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.name").value("iphone 16 pro"))
            .andExpect(jsonPath("$.price").value(99990.00));

        verify(service).getProduct(id);
    }

    @Test
    void shouldReturnBadRequestWhenProductDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.getProduct(id)).thenThrow(new ProductNotFoundException("Product not found"));

        mockMvc
            .perform(get("/api/products/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Product not found"));
    }

    @Test
    void shouldReturnProductCardsPage() throws Exception {
        UUID id = UUID.randomUUID();
        ProductCardResponse response = new ProductCardResponse(
            id,
            "iphone-16-pro",
            "iphone 16 pro",
            new BigDecimal("99990.00"),
            "/media/products/iphone.webp",
            ProductStatus.OFF
        );
        Pageable pageable = PageRequest.of(1, 2);

        when(service.getCatalogProductCards(any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(response), pageable, 3)
        );

        mockMvc
            .perform(
                get("/api/catalog/products").param("page", "1").param("size", "2").param("sort", "price,desc")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(id.toString()))
            .andExpect(jsonPath("$.content[0].name").value("iphone 16 pro"))
            .andExpect(jsonPath("$.page.totalElements").value(3));

        verify(service).getCatalogProductCards(any(Pageable.class));
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductDetailsResponse response = productDetailsResponse(productId);

        when(service.updateProduct(eq(productId), eq(10L), any(UpdateProductRequest.class))).thenReturn(
            response
        );

        mockMvc
            .perform(
                patch("/api/products/{id}", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                { "name": "New name" }
                                """
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(productId.toString()));

        verify(service).updateProduct(eq(productId), eq(10L), any(UpdateProductRequest.class));
    }

    @Test
    void shouldAddDetailsImages() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductDetailsImageResponse response = new ProductDetailsImageResponse(
            UUID.randomUUID(),
            "/media/new.webp",
            2
        );

        when(service.addProductDetailsImages(eq(productId), anyList(), eq(10L))).thenReturn(
            List.of(response)
        );

        mockMvc
            .perform(multipart("/api/products/{id}/details-images", productId).file(image("images")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$[0].uuid").value(response.uuid().toString()))
            .andExpect(jsonPath("$[0].position").value(2));

        verify(service).addProductDetailsImages(eq(productId), anyList(), eq(10L));
    }

    @Test
    void shouldUpdateImagesPositions() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        ProductDetailsImageResponse response = new ProductDetailsImageResponse(
            imageId,
            "/media/image.webp",
            0
        );

        when(
            service.updateProductImageOrder(eq(productId), any(UpdateProductImageOrderRequest.class), eq(10L))
        ).thenReturn(List.of(response));

        mockMvc
            .perform(
                patch("/api/products/{id}/details-images/positions", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                { "imageIds": ["%s"] }
                                """.formatted(imageId)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].uuid").value(imageId.toString()));

        verify(service).updateProductImageOrder(
            eq(productId),
            any(UpdateProductImageOrderRequest.class),
            eq(10L)
        );
    }

    @Test
    void shouldDeleteDetailsImage() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        ProductDetailsImageResponse response = new ProductDetailsImageResponse(
            imageId,
            "/media/image.webp",
            1
        );

        when(service.deleteProductDetailsImage(imageId, productId, 10L)).thenReturn(response);

        mockMvc
            .perform(delete("/api/products/{id}/details-images/{imageId}", productId, imageId))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        verify(service).deleteProductDetailsImage(imageId, productId, 10L);
    }

    @Test
    void shouldUpdatePreviewImage() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductCardResponse response = new ProductCardResponse(
            productId,
            "iphone-16-pro",
            "iphone 16 pro",
            new BigDecimal("99990.00"),
            "/media/new-preview.webp",
            ProductStatus.OFF
        );

        when(service.updateProductPreview(eq(productId), any(), eq(10L))).thenReturn(response);

        mockMvc
            .perform(
                multipart(HttpMethod.PUT, "/api/products/{id}/preview-image", productId).file(
                    image("previewImage")
                )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imgUrl").value("/media/new-preview.webp"));

        verify(service).updateProductPreview(eq(productId), any(), eq(10L));
    }

    @Test
    void shouldArchiveProduct() throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc
            .perform(post("/api/products/{id}/archive", productId))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        verify(service).archiveProduct(productId, 10L);
    }

    private ProductDetailsResponse productDetailsResponse(UUID productId) {
        return new ProductDetailsResponse(
            productId,
            "iphone 16 pro",
            "iphone-16-pro",
            new BigDecimal("99990.00"),
            "A sufficiently long product description for tests.",
            List.of(),
            null,
            ProductStatus.OFF,
            ModerationStatus.NOT_SUBMITTED
        );
    }

    private MockMultipartHttpServletRequestBuilder createProductRequest(String productJson) {
        return multipart("/api/products")
            .file(
                new MockMultipartFile(
                    "product",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    productJson.getBytes(StandardCharsets.UTF_8)
                )
            )
            .file(image("previewImage"))
            .file(image("detailsImages"));
    }

    private MockMultipartFile image(String partName) {
        return new MockMultipartFile(partName, "image.webp", "image/webp", "image".getBytes());
    }
}
