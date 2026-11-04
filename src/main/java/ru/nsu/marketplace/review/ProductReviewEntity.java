package ru.nsu.marketplace.review;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.product.model.ProductEntity;
import ru.nsu.marketplace.user.UserEntity;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "product_review_entity")
public class ProductReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_review_id_generator")
    @SequenceGenerator(
            name = "product_review_id_generator",
            sequenceName = "product_review_seq",
            allocationSize = 50
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int rating;

    @Length(min = 10, max = 2000)
    @Column(length = 2000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
