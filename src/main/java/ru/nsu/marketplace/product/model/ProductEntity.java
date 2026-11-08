package ru.nsu.marketplace.product.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.user.UserEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_id_generator")
    @SequenceGenerator(
            name = "product_id_generator",
            sequenceName = "products_seq",
            allocationSize = 50
    )
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID publicId;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private UserEntity seller;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.OFF;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModerationStatus moderationStatus = ModerationStatus.NOT_SUBMITTED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategoryEntity category;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @Builder.Default
    private List<ProductDetailsImageEntity> detailsImages = new ArrayList<>();
}
