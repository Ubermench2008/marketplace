package ru.nsu.marketplace.product.dto;

import java.math.BigDecimal;

public record CatalogFilter(
    String category,
    BigDecimal minPrice,
    BigDecimal maxPrice
) {}
