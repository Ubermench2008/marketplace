package ru.nsu.marketplace.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.cart.CartEntity;
import ru.nsu.marketplace.cart.CartRepository;
import ru.nsu.marketplace.common.phone.PhoneNumberService;
import ru.nsu.marketplace.user.dto.CreateUserRequest;
import ru.nsu.marketplace.user.dto.UpdateUserRequest;
import ru.nsu.marketplace.user.dto.UserResponse;
import ru.nsu.marketplace.user.exception.EmailAlreadyExistsException;
import ru.nsu.marketplace.user.exception.TelephoneAlreadyExistsException;
import ru.nsu.marketplace.user.exception.UserNotFoundException;

import java.util.HashSet;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhoneNumberService phoneNumberService;
    private final CartRepository cartRepository;

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return toResponse(findUser(userId));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String telephone = phoneNumberService.normalize(request.telephone());
        String email = normalizeEmail(request.email());

        ensureTelephoneAvailable(telephone, null);
        ensureEmailAvailable(email, null);

        UserEntity user = UserEntity.builder()
                .name(request.name())
                .telephoneNumber(telephone)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(new HashSet<>(request.roles()))
                .build();

        UserEntity savedUser = userRepository.save(user);

        if (!savedUser.getRoles().contains(Role.ADMIN)) {
            CartEntity cart = new CartEntity();
            cart.setUser(savedUser);

            cartRepository.save(cart);
        }

        log.info("User was created: userId={}, roles={}",
                savedUser.getId(), savedUser.getRoles());

        return toResponse(savedUser);
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

        log.info("User was updated: userId={}", userId);

        return toResponse(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        UserEntity user = findUser(userId);

        cartRepository.findByUser_Id(userId).ifPresent(cartRepository::delete);

        userRepository.delete(user);

        log.info("User was deleted: userId={}", userId);
    }

    private UserEntity findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );
    }

    private void ensureTelephoneAvailable(String telephone, Long excludedUserId) {
        userRepository.findByTelephoneNumber(telephone)
                .filter(user -> !user.getId().equals(excludedUserId))
                .ifPresent(user -> {
                    throw new TelephoneAlreadyExistsException(
                            "Telephone number is already registered"
                    );
                });
    }

    private void ensureEmailAvailable(String email, Long excludedUserId) {
        if (email == null) {
            return;
        }

        userRepository.findByEmail(email)
                .filter(user -> !user.getId().equals(excludedUserId))
                .ifPresent(user -> {
                    throw new EmailAlreadyExistsException(
                            "Email is already registered"
                    );
                });
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(UserEntity userEntity) {
        return new UserResponse(
                userEntity.getId(),
                userEntity.getName(),
                userEntity.getTelephoneNumber(),
                userEntity.getEmail(),
                userEntity.getRoles().stream().toList()
        );
    }
}
