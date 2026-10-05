package ru.nsu.marketplace.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.nsu.marketplace.dto.CreateUserRequest;
import ru.nsu.marketplace.dto.UpdateUserRequest;
import ru.nsu.marketplace.dto.UserResponse;
import ru.nsu.marketplace.entity.UserEntity;
import ru.nsu.marketplace.entity.CartEntity;
import ru.nsu.marketplace.enums.Role;
import ru.nsu.marketplace.exceptions.EmailAlreadyExistException;
import ru.nsu.marketplace.exceptions.NumberAlreadyExistException;
import ru.nsu.marketplace.exceptions.UserNotFoundException;
import ru.nsu.marketplace.repository.UserRepository;
import ru.nsu.marketplace.repository.CartRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PhoneNumberService phoneNumberService;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private UserService service;

    @Test
    void shouldReturnUsersPageMappedToResponse() {
        Pageable pageable = PageRequest.of(0, 20);
        UserEntity firstUser = user(1L, "first_user", "+79990000001", "first@example.com", Set.of(Role.BUYER));
        UserEntity secondUser = user(2L, "second_user", "+79990000002", null, Set.of(Role.BUYER, Role.SELLER));

        when(repository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(firstUser, secondUser), pageable, 2));

        Page<UserResponse> result = service.getUsers(pageable);

        assertThat(result.getContent()).extracting(UserResponse::id)
                .containsExactly(1L, 2L);
        assertThat(result.getContent()).extracting(UserResponse::name)
                .containsExactly("first_user", "second_user");
        assertThat(result.getContent()).extracting(UserResponse::telephone)
                .containsExactly("+79990000001", "+79990000002");
        assertThat(result.getContent()).extracting(UserResponse::email)
                .containsExactly("first@example.com", null);
        verify(repository).findAll(pageable);
    }

    @Test
    void shouldReturnUserMappedToResponse() {
        UserEntity user = user(1L, "buyer", "+79990000001", "buyer@example.com", Set.of(Role.BUYER));
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse result = service.getUser(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("buyer");
        assertThat(result.telephone()).isEqualTo("+79990000001");
        assertThat(result.email()).isEqualTo("buyer@example.com");
        assertThat(result.roles()).containsExactly(Role.BUYER);
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.getUser(99L));
    }

    @Test
    void shouldCreateUserWithNormalizedContactsAndEncodedPassword() {
        CreateUserRequest request = new CreateUserRequest(
                "seller_01",
                "+7 (999) 000-00-01",
                " Seller@Example.COM ",
                "Password123",
                Set.of(Role.BUYER, Role.SELLER)
        );
        when(phoneNumberService.normalize(request.telephone())).thenReturn("+79990000001");
        when(passwordEncoder.encode(request.password())).thenReturn("password-hash");
        when(repository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = service.createUser(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(repository).save(captor.capture());
        UserEntity savedUser = captor.getValue();

        assertThat(savedUser.getName()).isEqualTo("seller_01");
        assertThat(savedUser.getTelephoneNumber()).isEqualTo("+79990000001");
        assertThat(savedUser.getEmail()).isEqualTo("seller@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("password-hash");
        assertThat(savedUser.getRoles()).containsExactlyInAnyOrder(Role.BUYER, Role.SELLER);
        assertThat(result.name()).isEqualTo("seller_01");
        assertThat(result.email()).isEqualTo("seller@example.com");
        verify(passwordEncoder).encode("Password123");

        ArgumentCaptor<CartEntity> cartCaptor = ArgumentCaptor.forClass(CartEntity.class);
        verify(cartRepository).save(cartCaptor.capture());

        assertThat(cartCaptor.getValue().getUser()).isSameAs(savedUser);
        assertThat(cartCaptor.getValue().getCartItemEntities()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"BUYER", "SELLER"})
    void shouldCreateEmptyCartForNonAdminUser(Role role) {
        CreateUserRequest request = new CreateUserRequest(
                "new_user", "+79990000001", null, "Password123", Set.of(role)
        );
        when(phoneNumberService.normalize(request.telephone())).thenReturn("+79990000001");
        when(repository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createUser(request);

        ArgumentCaptor<CartEntity> captor = ArgumentCaptor.forClass(CartEntity.class);
        verify(cartRepository).save(captor.capture());

        assertThat(captor.getValue().getUser().getRoles()).containsExactly(role);
        assertThat(captor.getValue().getCartItemEntities()).isEmpty();
    }

    @Test
    void shouldNotCreateCartForAdminUser() {
        CreateUserRequest request = new CreateUserRequest(
                "new_admin", "+79990000001", null, "Password123", Set.of(Role.ADMIN)
        );
        when(phoneNumberService.normalize(request.telephone())).thenReturn("+79990000001");
        when(repository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = service.createUser(request);

        assertThat(result.roles()).containsExactly(Role.ADMIN);
        verifyNoInteractions(cartRepository);
    }

    @Test
    void shouldThrowWhenCreatingUserWithTakenTelephone() {
        CreateUserRequest request = createUserRequest();
        UserEntity existingUser = user(1L, "existing", "+79990000001", "existing@example.com", Set.of(Role.BUYER));
        when(phoneNumberService.normalize(request.telephone())).thenReturn("+79990000001");
        when(repository.findByTelephoneNumber("+79990000001")).thenReturn(Optional.of(existingUser));

        assertThrows(NumberAlreadyExistException.class, () -> service.createUser(request));

        verify(repository, never()).save(any(UserEntity.class));
        verifyNoInteractions(passwordEncoder, cartRepository);
    }

    @Test
    void shouldThrowWhenCreatingUserWithTakenEmail() {
        CreateUserRequest request = createUserRequest();
        UserEntity existingUser = user(1L, "existing", "+79990000002", "new@example.com", Set.of(Role.BUYER));
        when(phoneNumberService.normalize(request.telephone())).thenReturn("+79990000001");
        when(repository.findByEmail("new@example.com")).thenReturn(Optional.of(existingUser));

        assertThrows(EmailAlreadyExistException.class, () -> service.createUser(request));

        verify(repository, never()).save(any(UserEntity.class));
        verifyNoInteractions(passwordEncoder, cartRepository);
    }

    @Test
    void shouldUpdateProvidedFieldsOnly() {
        UserEntity user = user(1L, "old_name", "+79990000001", "old@example.com", Set.of(Role.BUYER));
        user.setPasswordHash("old-hash");
        UpdateUserRequest request = new UpdateUserRequest(
                "new_name",
                "+7 (999) 000-00-02",
                " New@Example.COM ",
                "NewPassword123",
                Set.of(Role.BUYER, Role.SELLER)
        );
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(phoneNumberService.normalize(request.telephone())).thenReturn("+79990000002");
        when(passwordEncoder.encode(request.password())).thenReturn("new-hash");

        UserResponse result = service.updateUser(1L, request);

        assertThat(user.getName()).isEqualTo("new_name");
        assertThat(user.getTelephoneNumber()).isEqualTo("+79990000002");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.BUYER, Role.SELLER);
        assertThat(result.roles()).containsExactlyInAnyOrder(Role.BUYER, Role.SELLER);
        verify(repository, never()).save(any(UserEntity.class));
    }

    @Test
    void shouldAllowUserToKeepOwnTelephoneAndEmail() {
        UserEntity user = user(1L, "buyer", "+79990000001", "buyer@example.com", Set.of(Role.BUYER));
        UpdateUserRequest request = new UpdateUserRequest(
                null,
                "+7 (999) 000-00-01",
                "BUYER@EXAMPLE.COM",
                null,
                null
        );
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(phoneNumberService.normalize(request.telephone())).thenReturn("+79990000001");
        when(repository.findByTelephoneNumber("+79990000001")).thenReturn(Optional.of(user));
        when(repository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));

        service.updateUser(1L, request);

        assertThat(user.getTelephoneNumber()).isEqualTo("+79990000001");
        assertThat(user.getEmail()).isEqualTo("buyer@example.com");
    }

    @Test
    void shouldThrowWhenUpdatingTelephoneTakenByAnotherUser() {
        UserEntity user = user(1L, "buyer", "+79990000001", "buyer@example.com", Set.of(Role.BUYER));
        UserEntity anotherUser = user(2L, "another", "+79990000002", "another@example.com", Set.of(Role.BUYER));
        UpdateUserRequest request = new UpdateUserRequest(null, "+7 (999) 000-00-02", null, null, null);
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(phoneNumberService.normalize(request.telephone())).thenReturn("+79990000002");
        when(repository.findByTelephoneNumber("+79990000002")).thenReturn(Optional.of(anotherUser));

        assertThrows(NumberAlreadyExistException.class, () -> service.updateUser(1L, request));

        assertThat(user.getTelephoneNumber()).isEqualTo("+79990000001");
    }

    @Test
    void shouldDeleteExistingUser() {
        UserEntity user = user(1L, "buyer", "+79990000001", "buyer@example.com", Set.of(Role.BUYER));
        CartEntity cart = new CartEntity();
        cart.setUser(user);

        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser_Id(1L)).thenReturn(Optional.of(cart));

        service.deleteUser(1L);

        var order = inOrder(cartRepository, repository);
        order.verify(cartRepository).delete(cart);
        order.verify(repository).delete(user);
    }

    @Test
    void shouldDeleteAdminWithoutCart() {
        UserEntity user = user(1L, "admin", "+79990000001", null, Set.of(Role.ADMIN));
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        service.deleteUser(1L);

        verify(repository).delete(user);
        verify(cartRepository, never()).delete(any(CartEntity.class));
    }

    private CreateUserRequest createUserRequest() {
        return new CreateUserRequest(
                "new_user",
                "+7 (999) 000-00-01",
                "new@example.com",
                "Password123",
                Set.of(Role.BUYER)
        );
    }

    private UserEntity user(
            Long id,
            String name,
            String telephone,
            String email,
            Set<Role> roles
    ) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName(name);
        user.setTelephoneNumber(telephone);
        user.setEmail(email);
        user.setPasswordHash("password-hash");
        user.setRoles(new HashSet<>(roles));
        return user;
    }
}
