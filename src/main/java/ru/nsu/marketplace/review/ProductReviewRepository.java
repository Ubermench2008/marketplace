package ru.nsu.marketplace.review;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.nsu.marketplace.common.moderation.ModerationStatus;

import java.util.Optional;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReviewEntity, Long> {
    @EntityGraph(attributePaths = {"user", "product"})
    Page<ProductReviewEntity> findAllByProduct_PublicIdAndModerationStatusAndDeletedAtIsNull(
            UUID productId, ModerationStatus moderationStatus, Pageable pageable
    );

    Optional<ProductReviewEntity> findByProduct_PublicIdAndUser_IdAndDeletedAtIsNull(UUID productId, Long userId);

    boolean existsByProduct_IdAndUser_IdAndDeletedAtIsNull(Long productId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select review from ProductReviewEntity review where review.id = :id")
    Optional<ProductReviewEntity> findByIdForUpdate(@Param("id") Long id);
}
