package ru.nsu.marketplace.review.reply.reaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.review.exception.ProductReviewStateException;
import ru.nsu.marketplace.review.exception.ReviewReplyNotFoundException;
import ru.nsu.marketplace.review.reaction.ReactionType;
import ru.nsu.marketplace.review.reply.ProductReviewReplyEntity;
import ru.nsu.marketplace.review.reply.ProductReviewReplyRepository;
import ru.nsu.marketplace.review.reply.ReplyStatus;
import ru.nsu.marketplace.review.reaction.exception.ReactionNotFoundException;
import ru.nsu.marketplace.user.UserEntity;
import ru.nsu.marketplace.user.UserRepository;
import ru.nsu.marketplace.user.exception.UserNotFoundException;

@Service
@RequiredArgsConstructor
public class ProductReviewReplyReactionService {
    private final ProductReviewReplyRepository replyRepository;
    private final ProductReviewReplyReactionRepository reactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void setReaction(Long replyId, Long userId, ReactionType type) {
        if (type == null) {
            throw new IllegalArgumentException("Reaction type is required");
        }
        ProductReviewReplyEntity reply = findReplyForUpdate(replyId);
        ensureReplyIsPublished(reply);
        UserEntity user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );
        ProductReviewReplyReactionEntity reaction = reactionRepository.findByReply_IdAndUser_Id(replyId, userId)
                .orElse(null);
        if (reaction == null) {
            reaction = new ProductReviewReplyReactionEntity();
            reaction.setReply(reply);
            reaction.setUser(user);
            reaction.setType(type);
            reactionRepository.save(reaction);
        } else {
            reaction.setType(type);
        }
    }

    @Transactional
    public void deleteReaction(Long replyId, Long userId) {
        findReplyForUpdate(replyId);
        ProductReviewReplyReactionEntity reaction = reactionRepository.findByReply_IdAndUser_Id(replyId, userId)
                .orElseThrow(() -> new ReactionNotFoundException("Reaction not found"));
        reactionRepository.delete(reaction);
    }

    private ProductReviewReplyEntity findReplyForUpdate(Long replyId) {
        return replyRepository.findByIdForUpdate(replyId).orElseThrow(
                () -> new ReviewReplyNotFoundException("Review reply not found")
        );
    }

    private void ensureReplyIsPublished(ProductReviewReplyEntity reply) {
        if (reply.getStatus() != ReplyStatus.PUBLISHED
                || reply.getReview().getDeletedAt() != null
                || reply.getReview().getModerationStatus() != ModerationStatus.APPROVED) {
            throw new ProductReviewStateException("Reactions are available only for published replies to published reviews");
        }
    }
}
