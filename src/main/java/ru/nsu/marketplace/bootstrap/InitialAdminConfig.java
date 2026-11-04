package ru.nsu.marketplace.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.nsu.marketplace.common.phone.InvalidPhoneNumberException;
import ru.nsu.marketplace.common.phone.PhoneNumberService;
import ru.nsu.marketplace.user.Role;
import ru.nsu.marketplace.user.UserEntity;
import ru.nsu.marketplace.user.UserRepository;

import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class InitialAdminConfig {
    private final InitialAdminProperties initialAdminProperties;
    private final PhoneNumberService phoneNumberService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner initialAdminRunner() {
        return args -> {
            String telephone;

            try {
                telephone = phoneNumberService.normalize(initialAdminProperties.telephone());
            } catch (InvalidPhoneNumberException exception) {
                log.error("Initial admin bootstrap failed: invalid telephone", exception);

                throw new InitialAdminInitializationException(
                        "Initial admin telephone is invalid",
                        exception
                );
            }

            if (userRepository.existsByTelephoneNumber(telephone)) {
                log.info("Admin bootstrap was skipped: admin already exists");

                return;
            }

            UserEntity admin = UserEntity.builder()
                    .name(initialAdminProperties.name())
                    .telephoneNumber(telephone)
                    .passwordHash(passwordEncoder.encode(initialAdminProperties.password()))
                    .roles(Set.of(Role.ADMIN))
                    .build();

            userRepository.save(admin);

            log.info("Admin bootstrap was created: id={}", admin.getId());
        };
    }
}
