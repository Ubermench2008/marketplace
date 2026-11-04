package ru.nsu.marketplace.review.reply;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import ru.nsu.marketplace.review.reaction.ReactionType;

import java.util.Optional;

public interface ProductReviewReplyRepository extends JpaRepository<ProductReviewReplyEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reply from ProductReviewReplyEntity reply where reply.id = :id")
    Optional<ProductReviewReplyEntity> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "author")
    @Query(value = """
            select reply from ProductReviewReplyEntity reply
            where reply.review.id = :reviewId and reply.status = :status
            order by (
                select count(reaction) from ProductReviewReplyReactionEntity reaction
                where reaction.reply = reply and reaction.type = :likeType
            ) desc, reply.id asc
            """,
            countQuery = """
            select count(reply) from ProductReviewReplyEntity reply
            where reply.review.id = :reviewId and reply.status = :status
            """)
    Page<ProductReviewReplyEntity> findByReviewIdAndStatusOrderByLikes(
            @Param("reviewId") Long reviewId,
            @Param("status") ReplyStatus status,
            @Param("likeType") ReactionType likeType,
            Pageable pageable
    );
    @EntityGraph(attributePaths = "author")
    Page<ProductReviewReplyEntity> findByReview_IdAndStatus(Long reviewId, ReplyStatus status, Pageable pageable);
    Page<ProductReviewReplyEntity> findByStatus(ReplyStatus status, Pageable pageable);
}
