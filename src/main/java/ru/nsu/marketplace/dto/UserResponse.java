package ru.nsu.marketplace.dto;

import ru.nsu.marketplace.enums.Role;

import java.util.List;

public record UserResponse(
        Long id,
        String name,
        String telephone,
        String email,
        List<Role> roles
) {}
