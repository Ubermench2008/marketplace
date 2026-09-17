package ru.nsu.marketplace.dto;

import jakarta.validation.constraints.*;
import ru.nsu.marketplace.enums.Role;

import java.util.Set;

public record AdminCreateUserRequest(
        @NotBlank
        @Size(min = 3, max = 32)
        @Pattern(regexp = "^[A-Za-z0-9_]+$")
        String name,

        @NotBlank
        @Pattern(regexp = "^\\+7\\d{10}$")
        String telephone,

        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @NotEmpty
        Set<Role> roles
) {}
