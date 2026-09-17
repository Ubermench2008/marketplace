package ru.nsu.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.dto.CreateUserRequest;
import ru.nsu.marketplace.dto.UpdateUserRequest;
import ru.nsu.marketplace.dto.UserResponse;
import ru.nsu.marketplace.entity.UserEntity;
import ru.nsu.marketplace.exceptions.EmailAlreadyExistException;
import ru.nsu.marketplace.exceptions.NumberAlreadyExistException;
import ru.nsu.marketplace.exceptions.UserNotFoundException;
import ru.nsu.marketplace.repository.UserRepository;
import ru.nsu.marketplace.service.PhoneNumberService;

import java.util.HashSet;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final PhoneNumberService phoneNumberService;

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return toDto(findUser(userId));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String telephone = phoneNumberService.normalize(request.telephone());
        String email = normalizeEmail(request.email());

        ensureTelephoneAvailable(telephone, null);
        ensureEmailAvailable(email, null);

        UserEntity user = new UserEntity();
        user.setName(request.name());
        user.setTelephoneNumber(telephone);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(new HashSet<>(request.roles()));

        return toDto(repository.save(user));
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        UserEntity user = findUser(userId);

        if (request.name() != null) {
            user.setName(request.name());
        }

        if (request.telephone() != null) {
            String telephone = phoneNumberService.normalize(request.telephone());
            ensureTelephoneAvailable(telephone, user.getId());
            user.setTelephoneNumber(telephone);
        }

        if (request.email() != null) {
            String email = normalizeEmail(request.email());
            ensureEmailAvailable(email, user.getId());
            user.setEmail(email);
        }

        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        if (request.roles() != null) {
            user.getRoles().clear();
            user.getRoles().addAll(request.roles());
        }

        return toDto(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        repository.delete(findUser(userId));
    }

    private UserEntity findUser(Long userId) {
        return repository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );
    }

    private void ensureTelephoneAvailable(String telephone, Long currentUserId) {
        repository.findByTelephoneNumber(telephone)
                .filter(user -> !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new NumberAlreadyExistException(
                            "Номер телефона уже зарегистрирован"
                    );
                });
    }

    private void ensureEmailAvailable(String email, Long currentUserId) {
        if (email == null) {
            return;
        }

        repository.findByEmail(email)
                .filter(user -> !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new EmailAlreadyExistException(
                            "Email уже зарегистрирован"
                    );
                });
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toDto(UserEntity userEntity) {
        return new UserResponse(
                userEntity.getId(),
                userEntity.getName(),
                userEntity.getTelephoneNumber(),
                userEntity.getEmail(),
                userEntity.getRoles().stream().toList()
        );
    }
}
