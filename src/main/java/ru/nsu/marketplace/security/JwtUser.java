package ru.nsu.marketplace.security;

import ru.nsu.marketplace.enums.Role;

import java.util.Set;

public record JwtUser(
        Long userId,
        Set<Role> userRoles
) {}
