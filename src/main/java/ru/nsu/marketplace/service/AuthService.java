package ru.nsu.marketplace.service;

import lombok.extern.slf4j.Slf4j;
import ru.nsu.marketplace.exceptions.EmailAlreadyExistException;
import ru.nsu.marketplace.exceptions.InvalidCredentialsException;
import ru.nsu.marketplace.exceptions.InvalidPhoneNumberException;
import ru.nsu.marketplace.exceptions.NumberAlreadyExistException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.dto.LoginRequest;
import ru.nsu.marketplace.dto.RegisterRequest;
import ru.nsu.marketplace.entity.UserEntity;
import ru.nsu.marketplace.entity.CartEntity;
import ru.nsu.marketplace.enums.Role;
import ru.nsu.marketplace.repository.UserRepository;
import ru.nsu.marketplace.repository.CartRepository;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final PhoneNumberService phoneNumberService;
    private final JwtService jwtService;
    private final CartRepository cartRepository;

    @Transactional
    public void register(RegisterRequest request) {
        String phoneNumber = phoneNumberService.normalize(request.telephone());
        String email = request.email();

        if (repository.existsByTelephoneNumber(phoneNumber)) {
            throw new NumberAlreadyExistException("Номер телефона уже зарегистрирован");
        }

        if (email != null && repository.existsByEmail(email)) {
            throw new EmailAlreadyExistException("Email уже зарегистрирован");
        }

        String passwordHash = encoder.encode(request.password());

        UserEntity newUser = new UserEntity();
        newUser.setName(request.name());
        newUser.setEmail(email);
        newUser.setTelephoneNumber(phoneNumber);
        newUser.setPasswordHash(passwordHash);
        newUser.setRoles(Set.of(Role.BUYER));

        UserEntity saved = repository.save(newUser);

        CartEntity cart = new CartEntity();
        cart.setUser(saved);

        cartRepository.save(cart);

        log.info("User registered: id={}", saved.getId());
    }

    public String login(LoginRequest request) {
        String login = request.login();
        String password = request.password();

        Optional<UserEntity> user;

        if (isEmail(login)) {
            user = repository.findByEmail(login);
        } else {
            String telephone;

            try {
                telephone = phoneNumberService.normalize(login);
            } catch (InvalidPhoneNumberException exception) {
                throw new InvalidCredentialsException("Неверный логин или пароль");
            }

            user = repository.findByTelephoneNumber(telephone);
        }

        UserEntity foundUser = user.orElseThrow(
                () -> new InvalidCredentialsException("Неверный логин или пароль")
        );

        if (!encoder.matches(password, foundUser.getPasswordHash())) {
            log.warn("Login failed: incorrect password for userId={}", foundUser.getId());

            throw new InvalidCredentialsException("Неверный логин или пароль");
        }

        log.info("User successfully authenticated: user={}", foundUser.getId());
        return jwtService.createAccessToken(foundUser);

    }

    private boolean isEmail(String login) {
        return login.contains("@");
    }
}
