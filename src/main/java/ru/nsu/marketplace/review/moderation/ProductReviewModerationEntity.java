package ru.nsu.marketplace.review.moderation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.review.ProductReviewEntity;
import ru.nsu.marketplace.user.UserEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_review_moderation_entity")
@Setter
@Getter
public class ProductReviewModerationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_review_moderation_generator")
    @SequenceGenerator(
            name = "product_review_moderation_generator",
            sequenceName = "product_review_moderation_seq",
            allocationSize = 50
    )
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_review_id", nullable = false, updatable = false)
    private ProductReviewEntity productReview;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModerationStatus status = ModerationStatus.PENDING;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private UserEntity reviewedBy;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "review_rating", nullable = false)
    private int reviewRating;

    @Column(name = "review_text", length = 2000)
    private String reviewText;

    @Column(name = "review_version", nullable = false)
    private long reviewVersion;
}
