package ru.nsu.marketplace.service;

import ru.nsu.marketplace.exceptions.EmailAlreadyExistException;
import ru.nsu.marketplace.exceptions.InvalidCredentialsException;
import ru.nsu.marketplace.exceptions.InvalidPhoneNumberException;
import ru.nsu.marketplace.exceptions.NumberAlreadyExistException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.nsu.marketplace.dto.LoginRequest;
import ru.nsu.marketplace.dto.RegisterRequest;
import ru.nsu.marketplace.entity.UserEntity;
import ru.nsu.marketplace.enums.Role;
import ru.nsu.marketplace.repository.UserRepository;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final PhoneNumberService phoneNumberService;
    private final JwtService jwtService;

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

        repository.save(newUser);
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
            throw new InvalidCredentialsException("Неверный логин или пароль");
        }

        return jwtService.createAccessToken(foundUser);

    }

    private boolean isEmail(String login) {
        return login.contains("@");
    }
}
