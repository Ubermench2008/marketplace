package ru.nsu.marketplace.review.reaction;

import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.review.ProductReviewEntity;
import ru.nsu.marketplace.review.ProductReviewRepository;
import ru.nsu.marketplace.review.exception.ProductReviewNotFoundException;
import ru.nsu.marketplace.review.exception.ProductReviewStateException;
import ru.nsu.marketplace.review.reaction.exception.ReactionNotFoundException;
import ru.nsu.marketplace.review.reaction.exception.ReactionAlreadyExistsException;
import ru.nsu.marketplace.user.UserEntity;
import ru.nsu.marketplace.user.UserRepository;
import ru.nsu.marketplace.user.exception.UserNotFoundException;

import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReviewReactionService {
    private final ProductReviewReactionRepository reviewReactionRepository;
    private final ProductReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MyReviewReactionResponse> getMyReactions(Long userId, List<Long> reviewIds) {
        if (reviewIds.size() > 100) {
            throw new IllegalArgumentException("At most 100 review IDs may be requested");
        }
        if (reviewIds.isEmpty()) {
            return List.of();
        }
        return reviewReactionRepository.findAllByUser_IdAndReview_IdInAndReview_DeletedAtIsNull(userId, reviewIds)
                .stream()
                .map(reaction -> new MyReviewReactionResponse(reaction.getReview().getId(), reaction.getType()))
                .toList();
    }

    @Transactional
    public void setReaction(Long reviewId, Long userId, ReactionType type) {
        UserEntity user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        ProductReviewEntity review = reviewRepository.findByIdForUpdate(reviewId).orElseThrow(
                () -> new ProductReviewNotFoundException("Product review not found")
        );

        if (review.getDeletedAt() != null || review.getModerationStatus() != ModerationStatus.APPROVED) {
            throw new ProductReviewStateException("Review must be APPROVED and exists for set reaction");
        }

        Optional<ProductReviewReactionEntity> reactionEntityOptional = reviewReactionRepository
                .findByReview_IdAndUser_Id(reviewId, userId);

        if (reactionEntityOptional.isEmpty()) {
            try {
                reviewReactionRepository.saveAndFlush(ProductReviewReactionEntity.builder()
                        .review(review)
                        .user(user)
                        .type(type)
                        .build());
            } catch (DataIntegrityViolationException exception) {
                Throwable cause = exception;
                while (cause != null) {
                    if (cause instanceof ConstraintViolationException violation
                            && "uk_product_review_reaction_review_user".equals(violation.getConstraintName())) {
                        throw new ReactionAlreadyExistsException("Reaction already exists. Retry the request", exception);
                    }
                    cause = cause.getCause();
                }
                throw exception;
            }
            return;
        }

        reactionEntityOptional.get().setType(type);
    }

    @Transactional
    public void deleteReaction(Long reviewId, Long userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        ProductReviewEntity review = reviewRepository.findByIdForUpdate(reviewId).orElseThrow(
                () -> new ProductReviewNotFoundException("Product review not found")
        );

        Optional<ProductReviewReactionEntity> reactionEntityOptional = reviewReactionRepository
                .findByReview_IdAndUser_Id(reviewId, userId);

        if (reactionEntityOptional.isEmpty()) {
            throw new ReactionNotFoundException("Reaction not found");
        }

        reviewReactionRepository.delete(reactionEntityOptional.get());
    }
}
