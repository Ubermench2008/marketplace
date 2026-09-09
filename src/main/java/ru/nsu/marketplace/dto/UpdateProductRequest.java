package ru.nsu.marketplace.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @Size(max = 150)
        @Pattern(
                regexp = "^[a-z0-9-]+$",
                message = "Slug must contain only lowercase letters, digits and hyphens"
        )
        String slug,

        @Size(max = 150)
        @Pattern(
                regexp = "^[^\\s](?:.*[^\\s])?$",
                message = "Name must not start or end with whitespace"
        )
        String name,

        @Size(min = 50)
        String description,

        @DecimalMin(
                value = "0.01",
                message = "Price must be greater than zero"
        )
        @Digits(integer = 9, fraction = 2)
        BigDecimal price
) {
}
