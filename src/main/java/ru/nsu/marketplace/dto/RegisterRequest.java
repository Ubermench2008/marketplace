package ru.nsu.marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.nsu.marketplace.validation.ValidPhoneNumber;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 32)
        @Pattern(
                regexp = "^[A-Za-z0-9_]+$",
                message = "Name may contain only Latin letters, digits and underscores"
        )
        String name,

        @NotBlank
        @ValidPhoneNumber
        String telephone,

        @Email(message = "Email must be valid")
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {}
