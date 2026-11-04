package ru.nsu.marketplace.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secretKey,
        Duration accessTokenTtl
) {}
