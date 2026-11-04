package ru.nsu.marketplace.review.reply.reaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.nsu.marketplace.review.reaction.ReactionType;

import java.util.Optional;
import java.util.List;

public interface ProductReviewReplyReactionRepository extends JpaRepository<ProductReviewReplyReactionEntity, Long> {
    Optional<ProductReviewReplyReactionEntity> findByReply_IdAndUser_Id(Long replyId, Long userId);

    List<ProductReviewReplyReactionEntity> findAllByReply_IdInAndUser_Id(List<Long> replyIds, Long userId);

    @Query("""
            select r.reply.id as replyId,
                count(case when r.type = :likeType then 1 else null end) as likes,
                count(case when r.type = :dislikeType then 1 else null end) as dislikes
            from ProductReviewReplyReactionEntity r
            where r.reply.id in :replyIds
            group by r.reply.id
            """)
    List<ReplyReactionCounts> countByReplyIds(
            @Param("replyIds") List<Long> replyIds,
            @Param("likeType") ReactionType likeType,
            @Param("dislikeType") ReactionType dislikeType
    );
}
