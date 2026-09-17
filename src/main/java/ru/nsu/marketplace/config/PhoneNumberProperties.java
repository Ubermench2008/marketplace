package ru.nsu.marketplace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "app.phone")
public record PhoneNumberProperties(
        Set<String> supportedRegions
) {}
