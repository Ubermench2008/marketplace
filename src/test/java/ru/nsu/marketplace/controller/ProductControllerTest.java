package ru.nsu.marketplace.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.nsu.marketplace.dto.CreateProductRequest;
import ru.nsu.marketplace.dto.ProductCardResponse;
import ru.nsu.marketplace.service.ProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService service;

    @Test
    public void shouldCreateProduct() throws Exception {
        ProductCardResponse response = new ProductCardResponse(
                UUID.randomUUID(),
                "iphone-16-pro",
                "iphone 16 pro",
                new BigDecimal("99990.00"),
                "/media/products/iphone.webp"
        );

        when(service.createProduct(any(CreateProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                post("/api/product/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "slug": "iphone-16-pro",
                          "name": "iphone 16 pro",
                          "description": "A sufficiently long product description for tests.",
                          "price": 99990.00,
                          "imgUrl": "/media/products/iphone.webp"
                        }
                        """)
                )
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
        mockMvc.perform(
                        post("/api/product/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                              "slug": "iphone-16-pro",
                              "name": "",
                              "price": 99990.00,
                              "imgUrl": "/media/products/iphone.webp"
                            }
                            """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnBadRequestWhenPriceIsNegative() throws Exception {
        mockMvc.perform(
                        post("/api/product/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                              "slug": "iphone-16-pro",
                              "name": "iphone 16 pro",
                              "price": -99990.00,
                              "imgUrl": "/media/products/iphone.webp"
                            }
                            """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnBadRequestWhenSlugIsNotExist() throws Exception {
        mockMvc.perform(
                        post("/api/product/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                              "name": "iphone 16 pro",
                              "price": -99990.00,
                              "imgUrl": "/media/products/iphone.webp"
                            }
                            """)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnProductCardById() throws Exception {
        UUID id = UUID.randomUUID();
        ProductCardResponse response = new ProductCardResponse(
                id,
                "iphone-16-pro",
                "iphone 16 pro",
                new BigDecimal("99990.00"),
                "/media/products/iphone.webp"
        );

        when(service.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/product/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("iphone 16 pro"))
                .andExpect(jsonPath("$.price").value(99990.00));

        verify(service).getById(id);
    }

    @Test
    void shouldReturnBadRequestWhenProductDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.getById(id))
                .thenThrow(new exceptions.ProductNotFoundException("Product not found"));

        mockMvc.perform(get("/api/product/{id}", id))
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

        when(service.getAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response), pageable, 3));

        mockMvc.perform(get("/api/product/all")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].name").value("iphone 16 pro"))
                .andExpect(jsonPath("$.totalElements").value(3));

        verify(service).getAll(any(Pageable.class));
    }
}
