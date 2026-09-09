package ru.nsu.marketplace.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record UpdateImagesPositionsRequest(
        @NotEmpty(message = "Image IDs are required")
        List<UUID> imageIds
) {}
