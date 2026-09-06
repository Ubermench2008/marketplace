package ru.nsu.marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.nsu.marketplace.dto.ProductCardResponse;
import ru.nsu.marketplace.entity.ProductCardEntity;
import ru.nsu.marketplace.repository.ProductCardsRepository;
import ru.nsu.marketplace.service.ProductService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class ProductCardIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProductService service;

    @Autowired
    ProductCardsRepository repository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldCreateCard() throws Exception {
        MvcResult postResult = mockMvc.perform(
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
                .andExpect(status().isOk())
                .andReturn();

        String jsonData = postResult.getResponse().getContentAsString();
        ProductCardResponse response = mapper.readValue(jsonData, ProductCardResponse.class);

        UUID uuid = response.id();

        ProductCardEntity entity = repository.findByPublicId(uuid).orElseThrow();

        assertThat(entity.getProduct().getName()).isEqualTo("iphone 16 pro");
        assertThat(entity.getProduct().getSlug()).isEqualTo("iphone-16-pro");
        assertThat(entity.getProduct().getPrice()).isEqualByComparingTo("99990.00");
        assertThat(entity.getImgUrl()).isEqualTo("/media/products/iphone.webp");

        mockMvc.perform(
                get("/api/product/{id}", uuid)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uuid.toString()))
                .andExpect(jsonPath("$.name").value(response.name()));

    }
}
