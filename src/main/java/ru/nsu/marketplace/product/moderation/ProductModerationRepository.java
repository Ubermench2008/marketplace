package ru.nsu.marketplace.product.moderation;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.nsu.marketplace.common.moderation.ModerationStatus;

import java.util.Optional;
import java.util.UUID;

public interface ProductModerationRepository extends JpaRepository<ProductModerationEntity, Long> {
    Page<ProductModerationEntity> findAllByStatus(ModerationStatus status, Pageable pageable);
    Optional<ProductModerationEntity> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from ProductModerationEntity request where request.publicId = :publicId")
    Optional<ProductModerationEntity> findByPublicIdForUpdate(@Param("publicId") UUID publicId);

    // Keep the product lookup in a subquery so the outer query locks only the request.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request
            from ProductModerationEntity request
            where request.product.id = (
                select product.id from ProductEntity product where product.publicId = :productId
            )
                and request.status = ru.nsu.marketplace.common.moderation.ModerationStatus.PENDING
            """)
    Optional<ProductModerationEntity> findPendingByProductPublicIdForUpdate(@Param("productId") UUID productId);

    @Query("""
            select count(request) > 0
            from ProductModerationEntity request
            where request.snapshot.previewImageUrl = :imageUrl
                or :imageUrl member of request.snapshotImageUrls
            """)
    boolean existsSnapshotReferencingImage(@Param("imageUrl") String imageUrl);

}
