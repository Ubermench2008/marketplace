package ru.nsu.marketplace.review.reaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductReviewReactionRepository extends JpaRepository<ProductReviewReactionEntity, Long> {
    Optional<ProductReviewReactionEntity> findByReview_IdAndUser_Id(Long reviewId, Long userId);

    List<ProductReviewReactionEntity> findAllByUser_IdAndReview_IdInAndReview_DeletedAtIsNull(
            Long userId, List<Long> reviewIds
    );

    @Query("""
        select
            r.review.id as reviewId,
            count(case when r.type = :likeType then 1 else null end) as likes,
            count(case when r.type = :dislikeType then 1 else null end) as dislikes
        from ProductReviewReactionEntity r
        where r.review.id in :reviewIds
        group by r.review.id
        """)
    List<ReviewReactionCountsByReview> countReactionsByReviewIds(
            @Param("reviewIds") List<Long> reviewIds,
            @Param("likeType") ReactionType likeType,
            @Param("dislikeType") ReactionType dislikeType);

    @Query("""
        select
            count(case when r.type = :likeType then 1 else null end) as likes,
            count(case when r.type = :dislikeType then 1 else null end) as dislikes
        from ProductReviewReactionEntity r
        where r.review.id = :reviewId
        """)
    ReviewReactionCounts countReactionsByReviewId(
            @Param("reviewId") Long reviewId,
            @Param("likeType") ReactionType likeType,
            @Param("dislikeType") ReactionType dislikeType);
}
