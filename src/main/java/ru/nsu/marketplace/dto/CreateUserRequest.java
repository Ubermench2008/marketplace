package ru.nsu.marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.nsu.marketplace.enums.Role;
import ru.nsu.marketplace.validation.ValidPhoneNumber;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank
        @Size(min = 3, max = 32)
        @Pattern(regexp = "^[A-Za-z0-9_]+$")
        String name,

        @NotBlank
        @ValidPhoneNumber
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
