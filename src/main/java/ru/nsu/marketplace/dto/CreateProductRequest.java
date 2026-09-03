package ru.nsu.marketplace.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "Slug is required")
        @Size(max = 150)
        @Pattern(
                regexp = "^[a-z0-9-]+$",
                message = "Slug must contain only lowercase letters, digits and hyphens"
        )
        String slug,

        @NotBlank
        @Size(max = 150)
        @Pattern(
                regexp = "^[^\\s](?:.*[^\\s])?$",
                message = "Name must not start or end with whitespace"
        )
        String name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 9, fraction = 2)
        BigDecimal price,

        @NotBlank(message = "Image URL is required")
        String imgUrl
) {}
