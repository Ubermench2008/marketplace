package ru.nsu.marketplace.cart.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddCartItemRequest(
        @NotNull UUID productId
) {}
