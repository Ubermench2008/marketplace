package ru.nsu.marketplace.product.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectProductModerationRequest(
        @NotBlank
        @Size(max = 2000)
        String reason
) {}
