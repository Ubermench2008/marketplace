package ru.nsu.marketplace.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.auth.dto.LoginRequest;
import ru.nsu.marketplace.auth.dto.RegisterRequest;
import ru.nsu.marketplace.cart.CartEntity;
import ru.nsu.marketplace.cart.CartRepository;
import ru.nsu.marketplace.common.phone.InvalidPhoneNumberException;
import ru.nsu.marketplace.common.phone.PhoneNumberService;
import ru.nsu.marketplace.user.Role;
import ru.nsu.marketplace.user.UserEntity;
import ru.nsu.marketplace.user.UserRepository;
import ru.nsu.marketplace.user.exception.EmailAlreadyExistsException;
import ru.nsu.marketplace.user.exception.TelephoneAlreadyExistsException;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhoneNumberService phoneNumberService;
    private final JwtService jwtService;
    private final CartRepository cartRepository;

    @Transactional
    public void register(RegisterRequest request) {
        String phoneNumber = phoneNumberService.normalize(request.telephone());
        String email = request.email();

        if (userRepository.existsByTelephoneNumber(phoneNumber)) {
            throw new TelephoneAlreadyExistsException("Telephone number is already registered");
        }

        if (email != null && userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());

        UserEntity newUser = UserEntity.builder()
                .name(request.name())
                .email(email)
                .telephoneNumber(phoneNumber)
                .passwordHash(passwordHash)
                .roles(Set.of(Role.BUYER))
                .build();

        UserEntity savedUser = userRepository.save(newUser);

        CartEntity cart = new CartEntity();
        cart.setUser(savedUser);

        cartRepository.save(cart);

        log.info("User registered: id={}", savedUser.getId());
    }

    public String login(LoginRequest request) {
        String login = request.login();
        String password = request.password();

        Optional<UserEntity> user;

        if (looksLikeEmail(login)) {
            user = userRepository.findByEmail(login);
        } else {
            String telephone;

            try {
                telephone = phoneNumberService.normalize(login);
            } catch (InvalidPhoneNumberException exception) {
                throw new InvalidCredentialsException("Invalid login or password");
            }

            user = userRepository.findByTelephoneNumber(telephone);
        }

        UserEntity foundUser = user.orElseThrow(
                () -> new InvalidCredentialsException("Invalid login or password")
        );

        if (!passwordEncoder.matches(password, foundUser.getPasswordHash())) {
            log.warn("Login failed: incorrect password for userId={}", foundUser.getId());

            throw new InvalidCredentialsException("Invalid login or password");
        }

        log.info("User successfully authenticated: user={}", foundUser.getId());
        return jwtService.createAccessToken(foundUser);

    }

    private boolean looksLikeEmail(String login) {
        return login.contains("@");
    }
}
