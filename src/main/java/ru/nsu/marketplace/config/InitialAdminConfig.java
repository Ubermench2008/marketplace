package ru.nsu.marketplace.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.nsu.marketplace.entity.UserEntity;
import ru.nsu.marketplace.enums.Role;
import ru.nsu.marketplace.exceptions.InitialAdminInitializationException;
import ru.nsu.marketplace.exceptions.InvalidPhoneNumberException;
import ru.nsu.marketplace.repository.UserRepository;
import ru.nsu.marketplace.service.PhoneNumberService;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class InitialAdminConfig {
    private final InitialAdminProperties properties;
    private final PhoneNumberService phoneNumberService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner createInitialAdmin() {
        return args -> {
            String telephone;

            try {
                telephone = phoneNumberService.normalize(properties.telephone());
            } catch (InvalidPhoneNumberException exception) {
                throw new InitialAdminInitializationException(
                        "Initial admin telephone is invalid",
                        exception
                );
            }

            if (userRepository.existsByTelephoneNumber(telephone)) {
                return;
            }

            UserEntity admin = new UserEntity();
            admin.setName(properties.name());
            admin.setTelephoneNumber(telephone);
            admin.setPasswordHash(passwordEncoder.encode(properties.password()));
            admin.setRoles(Set.of(Role.ADMIN));

            userRepository.save(admin);
        };
    }
}
