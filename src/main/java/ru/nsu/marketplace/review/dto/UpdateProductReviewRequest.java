package ru.nsu.marketplace.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProductReviewRequest(
        @Min(1)
        @Max(5)
        Integer rating,

        @Size(max = 2000)
        @Pattern(regexp = "(?s)(?:|.{10,})", message = "Text must be empty or contain at least 10 characters")
        String text
) {}
