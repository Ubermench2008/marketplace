package ru.nsu.marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.nsu.marketplace.dto.ProductDetailsResponse;
import ru.nsu.marketplace.entity.ProductEntity;
import ru.nsu.marketplace.repository.ProductCardsRepository;
import ru.nsu.marketplace.repository.ProductDetailsImageRepository;
import ru.nsu.marketplace.repository.ProductRepository;
import ru.nsu.marketplace.service.ProductService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public class ProductCardIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProductService service;

    @Autowired
    ProductRepository repository;

    @Autowired
    ProductCardsRepository productCardsRepository;

    @Autowired
    ProductDetailsImageRepository productDetailsImageRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldManageProductImagesAndDeleteProduct() throws Exception {
        MvcResult postResult = mockMvc.perform(
                        multipart("/api/products")
                                .file(new MockMultipartFile(
                                        "product", "", MediaType.APPLICATION_JSON_VALUE,
                                        """
                        {
                          "slug": "iphone-16-pro",
                          "name": "iphone 16 pro",
                          "description": "A sufficiently long product description for tests.",
                          "price": 99990.00
                        }
                        """.getBytes()
                                ))
                                .file(new MockMultipartFile(
                                        "previewImage", "preview.webp", "image/webp", "preview".getBytes()
                                ))
                                .file(new MockMultipartFile(
                                        "detailsImages", "details.webp", "image/webp", "details".getBytes()
                                ))
                )
                .andExpect(status().isOk())
                .andReturn();

        String jsonData = postResult.getResponse().getContentAsString();
        ProductDetailsResponse response = mapper.readValue(jsonData, ProductDetailsResponse.class);

        UUID uuid = response.id();

        ProductEntity entity = repository.findByPublicId(uuid).orElseThrow();

        assertThat(entity.getName()).isEqualTo("iphone 16 pro");
        assertThat(entity.getSlug()).isEqualTo("iphone-16-pro");
        assertThat(entity.getPrice()).isEqualByComparingTo("99990.00");
        assertThat(response.imageUrlList()).hasSize(1);

        mockMvc.perform(
                get("/api/products/{id}", uuid)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uuid.toString()))
                .andExpect(jsonPath("$.name").value(response.name()));

        mockMvc.perform(
                        multipart("/api/products/{id}/details-images", uuid)
                                .file(new MockMultipartFile(
                                        "images", "second.webp", "image/webp", "second".getBytes()
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].position").value(1));

        JsonNode images = getImages(uuid);
        UUID firstImageId = UUID.fromString(images.get(0).get("uuid").asText());
        UUID secondImageId = UUID.fromString(images.get(1).get("uuid").asText());

        mockMvc.perform(
                        patch("/api/products/{id}/details-images/positions", uuid)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        { "imageIds": ["%s", "%s"] }
                                        """.formatted(secondImageId, firstImageId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(secondImageId.toString()))
                .andExpect(jsonPath("$[0].position").value(0));

        mockMvc.perform(delete("/api/products/{id}/details-images/{imageId}", uuid, secondImageId))
                .andExpect(status().isOk());

        JsonNode remainingImages = getImages(uuid);
        assertThat(remainingImages.size()).isEqualTo(1);
        assertThat(remainingImages.get(0).get("uuid").asText()).isEqualTo(firstImageId.toString());
        assertThat(remainingImages.get(0).get("position").asInt()).isZero();

        mockMvc.perform(delete("/api/products/{id}", uuid))
                .andExpect(status().isNoContent());

        assertThat(repository.findByPublicId(uuid)).isEmpty();
        assertThat(productCardsRepository.count()).isZero();
        assertThat(productDetailsImageRepository.count()).isZero();

    }

    private JsonNode getImages(UUID productId) throws Exception {
        String body = mockMvc.perform(get("/api/products/{id}/details-images", productId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return mapper.readTree(body);
    }
}
