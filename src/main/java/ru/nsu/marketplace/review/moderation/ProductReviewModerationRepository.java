package ru.nsu.marketplace.review.moderation;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.nsu.marketplace.common.moderation.ModerationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductReviewModerationRepository extends JpaRepository<ProductReviewModerationEntity, Long> {
    @EntityGraph(attributePaths = {"productReview", "productReview.product", "productReview.user", "reviewedBy"})
    Page<ProductReviewModerationEntity> findAllByStatus(ModerationStatus status, Pageable pageable);

    Optional<ProductReviewModerationEntity> findByPublicId(UUID publicId);

    Optional<ProductReviewModerationEntity> findFirstByProductReview_IdOrderBySubmittedAtDescIdDesc(Long reviewId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request from ProductReviewModerationEntity request
            where request.productReview.id = :reviewId
            order by request.submittedAt desc, request.id desc
            """)
    List<ProductReviewModerationEntity> findLatestByReviewIdForUpdate(@Param("reviewId") Long reviewId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from ProductReviewModerationEntity request where request.publicId = :publicId")
    Optional<ProductReviewModerationEntity> findByPublicIdForUpdate(@Param("publicId") UUID publicId);
}
