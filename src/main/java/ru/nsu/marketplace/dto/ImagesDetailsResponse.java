package ru.nsu.marketplace.dto;

import java.util.UUID;

public record ImagesDetailsResponse(
        UUID uuid,
        String imgUrl,
        int position
) {}
