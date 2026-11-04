package ru.nsu.marketplace.product.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record UpdateProductImageOrderRequest(
        @NotEmpty(message = "Image IDs are required")
        List<UUID> imageIds
) {}
