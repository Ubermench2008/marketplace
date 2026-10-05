package ru.nsu.marketplace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.nsu.marketplace.dto.CreateUserRequest;
import ru.nsu.marketplace.dto.RegisterRequest;
import ru.nsu.marketplace.dto.UserResponse;
import ru.nsu.marketplace.entity.CartEntity;
import ru.nsu.marketplace.entity.UserEntity;
import ru.nsu.marketplace.enums.Role;
import ru.nsu.marketplace.repository.CartRepository;
import ru.nsu.marketplace.repository.UserRepository;
import ru.nsu.marketplace.service.AuthService;
import ru.nsu.marketplace.service.UserService;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = {
        "app.jwt.secretKey=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "app.initial-admin.name=test_admin",
        "app.initial-admin.telephone=+79991234567",
        "app.initial-admin.password=AdminPassword123"
})
@Testcontainers
public class UserCartIntegrationTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockitoSpyBean
    private CartRepository cartRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Container
    @ServiceConnection
    private static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");

    @Test
    void shouldCreateEmptyCartDuringRegistration() {
        RegisterRequest request = new RegisterRequest(
                "registered_buyer", "+79991234568", null, "Password123"
        );

        authService.register(request);

        UserEntity user = userRepository.findByTelephoneNumber(request.telephone()).orElseThrow();
        assertThat(user.getRoles()).containsExactly(Role.BUYER);
        assertEmptyCart(user.getId());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"BUYER", "SELLER"})
    void shouldCreateEmptyCartForUserCreatedByAdmin(Role role) {
        String telephone = role == Role.BUYER ? "+79991234569" : "+79991234570";
        CreateUserRequest request = new CreateUserRequest(
                "created_user", telephone, null, "Password123", Set.of(role)
        );

        UserResponse user = userService.createUser(request);

        assertEmptyCart(user.id());
    }

    @Test
    void shouldNotCreateCartForBootstrapOrNewAdmin() {
        UserEntity initialAdmin = userRepository.findByTelephoneNumber("+79991234567").orElseThrow();
        assertThat(cartRepository.findByUser_Id(initialAdmin.getId())).isEmpty();

        UserResponse admin = userService.createUser(new CreateUserRequest(
                "new_admin", "+79991234571", null, "Password123", Set.of(Role.ADMIN)
        ));

        assertThat(cartRepository.findByUser_Id(admin.id())).isEmpty();
    }

    @Test
    void shouldRollbackRegistrationWhenCartCannotBeSaved() {
        RegisterRequest request = new RegisterRequest(
                "rollback_buyer", "+79991234572", null, "Password123"
        );
        long cartsBefore = cartRepository.count();
        doThrow(new DataAccessResourceFailureException("Cart storage unavailable"))
                .when(cartRepository).save(any(CartEntity.class));

        assertThrows(DataAccessResourceFailureException.class, () -> authService.register(request));

        assertThat(userRepository.findByTelephoneNumber(request.telephone())).isEmpty();
        assertThat(cartRepository.count()).isEqualTo(cartsBefore);
    }

    @Test
    void shouldRollbackUserCreationWhenCartCannotBeSaved() {
        CreateUserRequest request = new CreateUserRequest(
                "rollback_seller", "+79991234573", null, "Password123", Set.of(Role.SELLER)
        );
        long cartsBefore = cartRepository.count();
        doThrow(new DataAccessResourceFailureException("Cart storage unavailable"))
                .when(cartRepository).save(any(CartEntity.class));

        assertThrows(DataAccessResourceFailureException.class, () -> userService.createUser(request));

        assertThat(userRepository.findByTelephoneNumber(request.telephone())).isEmpty();
        assertThat(cartRepository.count()).isEqualTo(cartsBefore);
    }

    @Test
    void shouldDeleteCartTogetherWithUser() {
        UserResponse user = userService.createUser(new CreateUserRequest(
                "deleted_buyer", "+79991234574", null, "Password123", Set.of(Role.BUYER)
        ));

        userService.deleteUser(user.id());

        assertThat(userRepository.findById(user.id())).isEmpty();
        assertThat(cartRepository.findByUser_Id(user.id())).isEmpty();
    }

    private void assertEmptyCart(Long userId) {
        CartEntity cart = cartRepository.findByUser_Id(userId).orElseThrow();

        assertThat(cart.getUser().getId()).isEqualTo(userId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cart_items WHERE cart_id = ?", Long.class, cart.getId()
        )).isZero();
    }
}
