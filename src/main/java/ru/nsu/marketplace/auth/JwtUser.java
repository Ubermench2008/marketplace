package ru.nsu.marketplace.auth;

import ru.nsu.marketplace.user.Role;

import java.util.Set;

public record JwtUser(
        Long userId,
        Set<Role> userRoles
) {}
