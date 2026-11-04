package ru.nsu.marketplace.product.moderation;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.product.model.ProductEntity;
import ru.nsu.marketplace.user.UserEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product_moderation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductModerationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_moderation_id_generator")
    @SequenceGenerator(
            name = "product_moderation_id_generator",
            sequenceName = "product_moderation_seq",
            allocationSize = 50
    )
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private ProductEntity product;

    @Embedded
    @Setter(AccessLevel.NONE)
    private ProductModerationSnapshot snapshot;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_moderation_images", joinColumns = @JoinColumn(name = "moderation_id"))
    @OrderColumn(name = "position", nullable = false)
    @Column(name = "image_url", nullable = false)
    @Immutable
    @Builder.Default
    @Setter(AccessLevel.NONE)
    private List<String> snapshotImageUrls = new ArrayList<>();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModerationStatus status = ModerationStatus.PENDING;

    @Builder.Default
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private UserEntity reviewedBy;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;
}
