package ru.nsu.marketplace.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.mock.web.MockMultipartFile;
import ru.nsu.marketplace.dto.CreateProductRequest;
import ru.nsu.marketplace.dto.ProductCardResponse;
import ru.nsu.marketplace.dto.ProductDetailsResponse;
import ru.nsu.marketplace.dto.ImagesDetailsResponse;
import ru.nsu.marketplace.dto.UpdateImagesPositionsRequest;
import ru.nsu.marketplace.dto.UpdateProductRequest;
import ru.nsu.marketplace.service.ProductService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@WebMvcTest({ProductController.class, CatalogController.class})
@AutoConfigureMockMvc(addFilters = false)
public class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService service;

    @Test
    public void shouldCreateProduct() throws Exception {
        ProductDetailsResponse response = new ProductDetailsResponse(
                UUID.randomUUID(),
                "iphone 16 pro",
                "iphone-16-pro",
                new BigDecimal("99990.00"),
                "A sufficiently long product description for tests.",
                List.of()
        );

        when(service.createProduct(any(CreateProductRequest.class), any(), anyList()))
                .thenReturn(response);

        mockMvc.perform(createProductRequest("""
                        {
                          "slug": "iphone-16-pro",
                          "name": "iphone 16 pro",
                          "description": "A sufficiently long product description for tests.",
                          "price": 99990.00
                        }
                        """))
                .andExpect(status()
                        .isOk())
                .andExpect(jsonPath("$.slug")
                        .value("iphone-16-pro"))
                .andExpect(jsonPath("$.name")
                        .value("iphone 16 pro"))
                .andExpect(jsonPath("$.price")
                        .value(99990.00));

    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        mockMvc.perform(createProductRequest("""
                            {
                              "slug": "iphone-16-pro",
                              "name": "",
                              "description": "A sufficiently long product description for tests.",
                              "price": 99990.00
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnBadRequestWhenPriceIsNegative() throws Exception {
        mockMvc.perform(createProductRequest("""
                            {
                              "slug": "iphone-16-pro",
                              "name": "iphone 16 pro",
                              "description": "A sufficiently long product description for tests.",
                              "price": -99990.00
                            }
                            """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnBadRequestWhenSlugIsNotExist() throws Exception {
        mockMvc.perform(createProductRequest("""
                            {
                              "name": "iphone 16 pro",
                              "description": "A sufficiently long product description for tests.",
                              "price": -99990.00
                            }
                            """))
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
                List.of()
        );

        when(service.getProduct(id)).thenReturn(response);

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("iphone 16 pro"))
                .andExpect(jsonPath("$.price").value(99990.00));

        verify(service).getProduct(id);
    }

    @Test
    void shouldReturnBadRequestWhenProductDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.getProduct(id))
                .thenThrow(new exceptions.ProductNotFoundException("Product not found"));

        mockMvc.perform(get("/api/products/{id}", id))
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
                "/media/products/iphone.webp"
        );
        Pageable pageable = PageRequest.of(1, 2);

        when(service.getCatalog(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), pageable, 3));

        mockMvc.perform(get("/api/catalog/products")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].name").value("iphone 16 pro"))
                .andExpect(jsonPath("$.totalElements").value(3));

        verify(service).getCatalog(any(Pageable.class));
    }

    @Test
    void shouldUpdateProduct() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductDetailsResponse response = productDetailsResponse(productId);

        when(service.updateProduct(eq(productId), any(UpdateProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "New name" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.toString()));

        verify(service).updateProduct(eq(productId), any(UpdateProductRequest.class));
    }

    @Test
    void shouldAddDetailsImages() throws Exception {
        UUID productId = UUID.randomUUID();
        ImagesDetailsResponse response = new ImagesDetailsResponse(
                UUID.randomUUID(), "/media/new.webp", 2
        );

        when(service.addImages(eq(productId), anyList())).thenReturn(List.of(response));

        mockMvc.perform(multipart("/api/products/{id}/details-images", productId)
                        .file(image("images")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(response.uuid().toString()))
                .andExpect(jsonPath("$[0].position").value(2));

        verify(service).addImages(eq(productId), anyList());
    }

    @Test
    void shouldUpdateImagesPositions() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        ImagesDetailsResponse response = new ImagesDetailsResponse(imageId, "/media/image.webp", 0);

        when(service.updateImagesPositions(eq(productId), any(UpdateImagesPositionsRequest.class)))
                .thenReturn(List.of(response));

        mockMvc.perform(patch("/api/products/{id}/details-images/positions", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "imageIds": ["%s"] }
                                """.formatted(imageId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(imageId.toString()));

        verify(service).updateImagesPositions(eq(productId), any(UpdateImagesPositionsRequest.class));
    }

    @Test
    void shouldDeleteDetailsImage() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        ImagesDetailsResponse response = new ImagesDetailsResponse(imageId, "/media/image.webp", 1);

        when(service.deleteProductDetailsImage(imageId, productId)).thenReturn(response);

        mockMvc.perform(delete("/api/products/{id}/details-images/{imageId}", productId, imageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(imageId.toString()));

        verify(service).deleteProductDetailsImage(imageId, productId);
    }

    @Test
    void shouldUpdatePreviewImage() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductCardResponse response = new ProductCardResponse(
                productId, "iphone-16-pro", "iphone 16 pro",
                new BigDecimal("99990.00"), "/media/new-preview.webp"
        );

        when(service.updatePreview(eq(productId), any())).thenReturn(response);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/products/{id}/preview-image", productId)
                        .file(image("previewImage")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imgUrl").value("/media/new-preview.webp"));

        verify(service).updatePreview(eq(productId), any());
    }

    @Test
    void shouldDeleteProduct() throws Exception {
        UUID productId = UUID.randomUUID();

        mockMvc.perform(delete("/api/products/{id}", productId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).deleteProduct(productId);
    }

    private ProductDetailsResponse productDetailsResponse(UUID productId) {
        return new ProductDetailsResponse(
                productId,
                "iphone 16 pro",
                "iphone-16-pro",
                new BigDecimal("99990.00"),
                "A sufficiently long product description for tests.",
                List.of()
        );
    }

    private MockMultipartHttpServletRequestBuilder createProductRequest(String productJson) {
        return multipart("/api/products")
                .file(new MockMultipartFile(
                        "product", "", MediaType.APPLICATION_JSON_VALUE,
                        productJson.getBytes(StandardCharsets.UTF_8)
                ))
                .file(image("previewImage"))
                .file(image("detailsImages"));
    }

    private MockMultipartFile image(String partName) {
        return new MockMultipartFile(partName, "image.webp", "image/webp", "image".getBytes());
    }
}
