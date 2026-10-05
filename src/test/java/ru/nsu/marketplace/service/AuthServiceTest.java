package ru.nsu.marketplace.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.nsu.marketplace.dto.LoginRequest;
import ru.nsu.marketplace.dto.RegisterRequest;
import ru.nsu.marketplace.entity.UserEntity;
import ru.nsu.marketplace.entity.CartEntity;
import ru.nsu.marketplace.enums.Role;
import ru.nsu.marketplace.exceptions.EmailAlreadyExistException;
import ru.nsu.marketplace.exceptions.InvalidCredentialsException;
import ru.nsu.marketplace.exceptions.InvalidPhoneNumberException;
import ru.nsu.marketplace.exceptions.NumberAlreadyExistException;
import ru.nsu.marketplace.repository.UserRepository;
import ru.nsu.marketplace.repository.CartRepository;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository repository;

    @Mock
    private PhoneNumberService phoneNumberService;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private AuthService service;

    @Test
    void shouldCreateBuyerUserRegisterTest() {
        RegisterRequest request = new RegisterRequest(
                "SomeUser",
                "+79144587954",
                "somemail@gmail.com",
                "Qwerty123"
        );

        when(phoneNumberService.normalize(request.telephone()))
                .thenReturn("+79144587954");

        when(repository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.register(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);

        verify(repository).save(captor.capture());

        UserEntity entity = captor.getValue();

        assertThat(entity.getRoles()).isEqualTo(Set.of(Role.BUYER));

        ArgumentCaptor<CartEntity> cartCaptor = ArgumentCaptor.forClass(CartEntity.class);

        verify(cartRepository).save(cartCaptor.capture());

        CartEntity cart = cartCaptor.getValue();

        assertThat(cart.getUser()).isSameAs(entity);
        assertThat(cart.getCartItemEntities()).isEmpty();
    }

    @Test
    void shouldThrowWhenTelephoneAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "SomeUser",
                "+79144587954",
                "somemail@gmail.com",
                "Qwerty123"
        );

        when(phoneNumberService.normalize(request.telephone()))
                .thenReturn("+79144587954");

        when(repository.existsByTelephoneNumber("+79144587954"))
                .thenReturn(true);

        NumberAlreadyExistException exception = assertThrows(NumberAlreadyExistException.class,
                () -> service.register(request));

        assertThat(exception.getMessage()).isEqualTo("Номер телефона уже зарегистрирован");
        verify(repository, never()).save(any(UserEntity.class));
        verifyNoInteractions(cartRepository);
    }

    @Test
    void shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "SomeUser",
                "+79144587954",
                "somemail@gmail.com",
                "Qwerty123"
        );

        when(phoneNumberService.normalize(request.telephone()))
                .thenReturn("+79144587954");

        when(repository.existsByEmail("somemail@gmail.com"))
                .thenReturn(true);

        EmailAlreadyExistException exception = assertThrows(EmailAlreadyExistException.class,
                () -> service.register(request));

        assertThat(exception.getMessage()).isEqualTo("Email уже зарегистрирован");
        verify(repository, never()).save(any(UserEntity.class));
        verifyNoInteractions(cartRepository);
    }

    @Test
    void shouldCallCreateAccessToken() {
        LoginRequest request = new LoginRequest(
                "correctLogin@gmail.com",
                "correctPassword"
        );

        UserEntity user = new UserEntity();
        user.setId(10L);
        user.setEmail("correctLogin@gmail.com");
        user.setPasswordHash("correctHashedPassword");
        user.setRoles(Set.of(Role.BUYER));

        when(repository.findByEmail("correctLogin@gmail.com"))
                .thenReturn(Optional.of(user));

        when(encoder.matches(request.password(), user.getPasswordHash()))
                .thenReturn(true);

        when(jwtService.createAccessToken(user))
                .thenReturn("accessToken");

        String token = service.login(request);

        assertThat(token).isEqualTo("accessToken");

        verify(jwtService).createAccessToken(user);
    }

    @Test
    void shouldThrowWhenLoginIsNotExist() {
        LoginRequest request = new LoginRequest(
                "incorrectLogin@gmail.com",
                "randomPassword"
        );

        when(repository.findByEmail("incorrectLogin@gmail.com"))
                .thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> service.login(request));

        assertThat(exception.getMessage()).isEqualTo("Неверный логин или пароль");

        verifyNoInteractions(encoder, jwtService);
    }

    @Test
    void shouldThrowWhenPasswordIsIncorrect() {
        LoginRequest request = new LoginRequest(
                "correctLogin@gmail.com",
                "incorrectPassword"
        );

        UserEntity user = new UserEntity();
        user.setId(10L);
        user.setEmail("correctLogin@gmail.com");
        user.setPasswordHash("correctHashedPassword");
        user.setRoles(Set.of(Role.BUYER));

        when(repository.findByEmail("correctLogin@gmail.com"))
                .thenReturn(Optional.of(user));

        when(encoder.matches(request.password(), "correctHashedPassword"))
                .thenReturn(false);

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> service.login(request));

        assertThat(exception.getMessage()).isEqualTo("Неверный логин или пароль");

        verifyNoInteractions(jwtService);
        verify(encoder).matches(
                "incorrectPassword",
                "correctHashedPassword"
        );
    }

    @Test
    void shouldThrowWhenLoginIsIncorrectTelephoneNumber() {
        LoginRequest request = new LoginRequest(
                "+777",
                "randomPassword"
        );

        when(phoneNumberService.normalize("+777"))
                .thenThrow(InvalidPhoneNumberException.class);

        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                () -> service.login(request));

        assertThat(exception.getMessage()).isEqualTo("Неверный логин или пароль");

        verifyNoInteractions(repository, encoder, jwtService);
    }
}
