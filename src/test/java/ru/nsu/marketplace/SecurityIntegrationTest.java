package ru.nsu.marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.nsu.marketplace.dto.LoginResponse;
import ru.nsu.marketplace.entity.UserEntity;
import ru.nsu.marketplace.enums.Role;
import ru.nsu.marketplace.repository.UserRepository;
import ru.nsu.marketplace.repository.CartRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class SecurityIntegrationTest {
    private static final String ADMIN_TELEPHONE = "+79991234567";
    private static final String ADMIN_PASSWORD = "AdminPassword123";
    private static final String BUYER_TELEPHONE = "+79991234568";
    private static final String BUYER_PASSWORD = "BuyerPassword123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Container
    @ServiceConnection
    private static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");

    private final ObjectMapper mapper = new ObjectMapper();

    @DynamicPropertySource
    static void configureApplicationProperties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-key", () -> "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
        registry.add("app.initial-admin.name", () -> "test_admin");
        registry.add("app.initial-admin.telephone", () -> ADMIN_TELEPHONE);
        registry.add("app.initial-admin.password", () -> ADMIN_PASSWORD);
    }

    @Test
    void shouldRestrictAdminEndpointsByRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "buyer_01",
                                  "telephone": "%s",
                                  "email": "buyer@example.com",
                                  "password": "%s"
                                }
                                """.formatted(BUYER_TELEPHONE, BUYER_PASSWORD)))
                .andExpect(status().isCreated());

        UserEntity buyer = userRepository.findByTelephoneNumber(BUYER_TELEPHONE).orElseThrow();
        assertThat(buyer.getRoles()).isEqualTo(Set.of(Role.BUYER));
        assertThat(cartRepository.findByUser_Id(buyer.getId())).isPresent();

        UserEntity admin = userRepository.findByTelephoneNumber(ADMIN_TELEPHONE).orElseThrow();
        assertThat(cartRepository.findByUser_Id(admin.getId())).isEmpty();

        String buyerToken = login(BUYER_TELEPHONE, BUYER_PASSWORD);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buyerToken))
                .andExpect(status().isForbidden());

        String adminToken = login(ADMIN_TELEPHONE, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    private String login(String login, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "%s",
                                  "password": "%s"
                                }
                                """.formatted(login, password)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse response = mapper.readValue(
                result.getResponse().getContentAsString(),
                LoginResponse.class
        );

        return response.accessToken();
    }
}
