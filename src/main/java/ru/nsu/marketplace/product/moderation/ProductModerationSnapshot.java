package ru.nsu.marketplace.product.moderation;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductModerationSnapshot {
    @Column(name = "snapshot_name", nullable = false, updatable = false)
    private String name;

    @Column(name = "snapshot_slug", nullable = false, updatable = false)
    private String slug;

    @Column(name = "snapshot_description", nullable = false, updatable = false, columnDefinition = "text")
    private String description;

    @Column(name = "snapshot_price", nullable = false, updatable = false, precision = 38, scale = 2)
    private BigDecimal price;

    @Column(name = "snapshot_preview_image_url", nullable = false, updatable = false)
    private String previewImageUrl;
}
