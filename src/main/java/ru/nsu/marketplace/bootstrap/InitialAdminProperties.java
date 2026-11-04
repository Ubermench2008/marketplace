package ru.nsu.marketplace.bootstrap;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.initial-admin")
public record InitialAdminProperties(
        @NotBlank
        String name,

        @NotBlank
        String telephone,

        @NotBlank
        String password
) {}
