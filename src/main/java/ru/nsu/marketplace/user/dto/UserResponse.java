package ru.nsu.marketplace.user.dto;

import ru.nsu.marketplace.user.Role;

import java.util.List;

public record UserResponse(
        Long id,
        String name,
        String telephone,
        String email,
        List<Role> roles
) {}
