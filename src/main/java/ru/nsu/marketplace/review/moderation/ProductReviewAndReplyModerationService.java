package ru.nsu.marketplace.review.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.review.exception.ReviewReplyNotFoundException;
import ru.nsu.marketplace.review.moderation.dto.ReviewReplyModerationResponse;
import ru.nsu.marketplace.review.reply.ProductReviewReplyEntity;
import ru.nsu.marketplace.review.reply.ProductReviewReplyRepository;
import ru.nsu.marketplace.review.reply.ReplyStatus;
import ru.nsu.marketplace.review.reply.dto.ReviewReplyStatusUpdateRequest;

@Service
@RequiredArgsConstructor
public class ProductReviewAndReplyModerationService {
    private final ProductReviewReplyRepository reviewReplyRepository;

    @Transactional
    public void updateReviewReplyStatus(Long reviewReplyId, ReviewReplyStatusUpdateRequest request) {
        if (request.status() == null) {
            throw new IllegalArgumentException("Reply status is required");
        }
        ProductReviewReplyEntity reviewReplyEntity = reviewReplyRepository
                .findByIdForUpdate(reviewReplyId)
                .orElseThrow(() -> new ReviewReplyNotFoundException("Review reply not found"));

        reviewReplyEntity.setStatus(request.status());
    }

    @Transactional(readOnly = true)
    public Page<ReviewReplyModerationResponse> getReviewRepliesForModeration(
            ReplyStatus status,
            Pageable pageable
    ) {
        return reviewReplyRepository.findByStatus(status, pageable).map(this::toDto);
    }

    private ReviewReplyModerationResponse toDto(ProductReviewReplyEntity entity) {
        return new ReviewReplyModerationResponse(
                entity.getId(),
                entity.getAuthor().getId(),
                entity.getReview().getId(),
                entity.getReview().getProduct().getId(),
                entity.getStatus(),
                entity.getAuthor().getName(),
                entity.getIsSeller(),
                entity.getReplyTo() == null ? null : entity.getReplyTo().getId(),
                entity.getText(),
                entity.getCreatedAt()
        );
    }
}
