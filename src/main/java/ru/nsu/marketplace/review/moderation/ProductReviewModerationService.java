package ru.nsu.marketplace.review.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.common.moderation.ReviewerResponse;
import ru.nsu.marketplace.review.ProductReviewEntity;
import ru.nsu.marketplace.review.ProductReviewRepository;
import ru.nsu.marketplace.review.exception.ProductReviewNotFoundException;
import ru.nsu.marketplace.review.exception.ProductReviewStateException;
import ru.nsu.marketplace.review.moderation.dto.ProductReviewModerationResponse;
import ru.nsu.marketplace.user.UserEntity;
import ru.nsu.marketplace.user.UserRepository;
import ru.nsu.marketplace.user.exception.UserNotFoundException;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductReviewModerationService {
    private final ProductReviewModerationRepository reviewModerationRepository;
    private final ProductReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<ProductReviewModerationResponse> getRequestsByStatus(
            ModerationStatus status,
            Pageable pageable
    ) {
        return reviewModerationRepository.findAllByStatus(status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductReviewModerationResponse getRequest(UUID requestId) {
        return toResponse(reviewModerationRepository.findByPublicId(requestId).orElseThrow(
                () -> new ProductReviewNotFoundException("Review moderation request not found")
        ));
    }

    @Transactional
    public void approveRequest(UUID requestId, Long moderatorId, long reviewVersion) {
        reviewRequest(requestId, moderatorId, reviewVersion, ModerationStatus.APPROVED, null);
    }

    @Transactional
    public void rejectRequest(UUID requestId, Long moderatorId, long reviewVersion, String reason) {
        reviewRequest(requestId, moderatorId, reviewVersion, ModerationStatus.REJECTED, reason);
    }

    private void reviewRequest(
            UUID requestId,
            Long moderatorId,
            long reviewVersion,
            ModerationStatus status,
            String reason
    ) {
        ProductReviewModerationEntity request = reviewModerationRepository
                .findByPublicIdForUpdate(requestId)
                .orElseThrow(() -> new ProductReviewNotFoundException("Review moderation request not found"));
        ensureModerationRequestIsPending(request);
        ensureReviewVersionMatches(request, reviewVersion);

        ProductReviewEntity review = reviewRepository
                .findByIdForUpdate(request.getProductReview().getId())
                .orElseThrow(() -> new ProductReviewNotFoundException("Review not found"));
        UserEntity moderator = userRepository.findById(moderatorId).orElseThrow(
                () -> new UserNotFoundException("Moderator not found")
        );
        request.setStatus(status);
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(moderator);
        request.setRejectionReason(reason);
        review.setModerationStatus(status);
    }

    private void ensureModerationRequestIsPending(ProductReviewModerationEntity request) {
        if (request.getStatus() != ModerationStatus.PENDING) {
            throw new ProductReviewStateException("Review moderation request has already been reviewed");
        }
    }

    private void ensureReviewVersionMatches(ProductReviewModerationEntity request, long reviewVersion) {
        if (request.getReviewVersion() != reviewVersion) {
            throw new ProductReviewStateException("Review has changed. Reload the moderation request before making a decision");
        }
    }

    private ProductReviewModerationResponse toResponse(ProductReviewModerationEntity request) {
        ProductReviewEntity review = request.getProductReview();
        UserEntity moderator = request.getReviewedBy();
        ReviewerResponse reviewer = moderator == null ? null : new ReviewerResponse(
                moderator.getName(), Set.copyOf(moderator.getRoles())
        );
        return new ProductReviewModerationResponse(
                request.getPublicId(),
                review.getId(),
                review.getProduct().getPublicId(),
                review.getUser().getId(),
                review.getUser().getName(),
                request.getReviewRating(),
                request.getReviewText(),
                request.getReviewVersion(),
                request.getStatus(),
                request.getSubmittedAt(),
                request.getReviewedAt(),
                reviewer,
                request.getRejectionReason()
        );
    }
}
