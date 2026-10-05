package ru.nsu.marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.nsu.marketplace.dto.CartResponse;
import ru.nsu.marketplace.dto.LoginResponse;
import ru.nsu.marketplace.entity.ProductCardEntity;
import ru.nsu.marketplace.entity.ProductEntity;
import ru.nsu.marketplace.repository.CartRepository;
import ru.nsu.marketplace.repository.ProductCardsRepository;
import ru.nsu.marketplace.repository.ProductRepository;
import ru.nsu.marketplace.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.jwt.secretKey=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "app.initial-admin.name=test_admin",
        "app.initial-admin.telephone=+79991234567",
        "app.initial-admin.password=AdminPassword123",
        "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@Testcontainers
public class CartIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCardsRepository productCardsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Container
    @ServiceConnection
    private static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");

    @Test
    void shouldPersistCartItemsAndDeleteThemWhenCartIsCleared() throws Exception {
        String buyerToken = registerAndLogin("cart_buyer", "+79991234568");
        String otherBuyerToken = registerAndLogin("other_buyer", "+79991234569");
        Long buyerId = userRepository.findByTelephoneNumber("+79991234568").orElseThrow().getId();
        Long otherBuyerId = userRepository.findByTelephoneNumber("+79991234569").orElseThrow().getId();
        Long cartId = cartRepository.findByUser_Id(buyerId).orElseThrow().getId();
        Long otherCartId = cartRepository.findByUser_Id(otherBuyerId).orElseThrow().getId();
        ProductEntity firstProduct = product("first-product", "19.90");
        ProductEntity secondProduct = product("second-product", "5.25");

        CartResponse initialCart = getCart(buyerToken);
        assertThat(initialCart.items()).isEmpty();
        assertThat(initialCart.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);

        mockMvc.perform(post("/api/cart/{productId}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken))
                .andExpect(status().isNotFound());
        assertThat(itemsCount(cartId)).isZero();

        addProduct(buyerToken, firstProduct.getPublicId());
        assertThat(itemsCount(cartId)).isEqualTo(1);
        Long firstItemId = jdbcTemplate.queryForObject(
                "SELECT id FROM cart_items WHERE cart_id = ? AND product_id = ?",
                Long.class, cartId, firstProduct.getId()
        );

        addProduct(buyerToken, firstProduct.getPublicId());
        assertThat(itemsCount(cartId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT quantity FROM cart_items WHERE id = ?", Integer.class, firstItemId
        )).isEqualTo(2);

        addProduct(buyerToken, secondProduct.getPublicId());
        addProduct(otherBuyerToken, firstProduct.getPublicId());

        CartResponse cart = getCart(buyerToken);
        assertThat(cart.items()).hasSize(2);
        assertThat(cart.items()).extracting(item -> item.product().id())
                .containsExactlyInAnyOrder(firstProduct.getPublicId(), secondProduct.getPublicId());
        assertThat(cart.items()).filteredOn(item -> item.product().id().equals(firstProduct.getPublicId()))
                .singleElement().satisfies(item -> {
                    assertThat(item.id()).isEqualTo(firstItemId);
                    assertThat(item.quantity()).isEqualTo(2);
                    assertThat(item.subtotal()).isEqualByComparingTo("39.80");
                });
        assertThat(cart.items()).filteredOn(item -> item.product().id().equals(secondProduct.getPublicId()))
                .singleElement().satisfies(item -> {
                    assertThat(item.quantity()).isEqualTo(1);
                    assertThat(item.subtotal()).isEqualByComparingTo("5.25");
                });
        assertThat(cart.totalPrice()).isEqualByComparingTo("45.05");

        String adminToken = login("+79991234567", "AdminPassword123");
        String adminCartJson = mockMvc.perform(get("/api/cart/{userId}", buyerId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        CartResponse adminCart = mapper.readValue(adminCartJson, CartResponse.class);
        assertThat(adminCart.items()).containsExactlyInAnyOrderElementsOf(cart.items());
        assertThat(adminCart.totalPrice()).isEqualByComparingTo(cart.totalPrice());

        mockMvc.perform(get("/api/cart/{userId}", otherBuyerId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        CartResponse emptyCart = getCart(buyerToken);
        assertThat(emptyCart.items()).isEmpty();
        assertThat(emptyCart.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(itemsCount(cartId)).isZero();
        assertThat(cartRepository.findById(cartId)).isPresent();
        assertThat(productRepository.findById(firstProduct.getId())).isPresent();
        assertThat(productRepository.findById(secondProduct.getId())).isPresent();
        assertThat(productCardsRepository.findByProduct_PublicId(firstProduct.getPublicId())).isPresent();
        assertThat(productCardsRepository.findByProduct_PublicId(secondProduct.getPublicId())).isPresent();

        assertThat(itemsCount(otherCartId)).isEqualTo(1);
        CartResponse otherCart = getCart(otherBuyerToken);
        assertThat(otherCart.items()).hasSize(1);
        assertThat(otherCart.items().getFirst().quantity()).isEqualTo(1);
        assertThat(otherCart.totalPrice()).isEqualByComparingTo("19.90");

        mockMvc.perform(delete("/api/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken))
                .andExpect(status().isNoContent());
        assertThat(itemsCount(cartId)).isZero();
        assertThat(cartRepository.findById(cartId)).isPresent();
    }

    private String registerAndLogin(String name, String telephone) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "telephone": "%s",
                                  "password": "Password123"
                                }
                                """.formatted(name, telephone)))
                .andExpect(status().isCreated());

        return login(telephone, "Password123");
    }

    private String login(String telephone, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "login": "%s", "password": "%s" }
                                """.formatted(telephone, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return mapper.readValue(response, LoginResponse.class).accessToken();
    }

    private void addProduct(String token, UUID productId) throws Exception {
        mockMvc.perform(post("/api/cart/{productId}", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    private CartResponse getCart(String token) throws Exception {
        String response = mockMvc.perform(get("/api/cart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return mapper.readValue(response, CartResponse.class);
    }

    private Long itemsCount(Long cartId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cart_items WHERE cart_id = ?", Long.class, cartId
        );
    }

    private ProductEntity product(String slug, String price) {
        ProductEntity product = new ProductEntity();
        product.setPublicId(UUID.randomUUID());
        product.setSlug(slug);
        product.setName(slug);
        product.setDescription("Product description for cart integration test.");
        product.setPrice(new BigDecimal(price));
        ProductEntity savedProduct = productRepository.save(product);

        ProductCardEntity card = new ProductCardEntity();
        card.setProduct(savedProduct);
        card.setImgUrl("/media/test-preview.webp");
        productCardsRepository.save(card);

        return savedProduct;
    }
}
