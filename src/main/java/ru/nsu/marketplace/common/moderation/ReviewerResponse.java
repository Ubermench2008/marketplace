package ru.nsu.marketplace.common.moderation;

import ru.nsu.marketplace.user.Role;

import java.util.Set;

public record ReviewerResponse(
        String reviewerName,
        Set<Role> reviewerRoles
) {
}
