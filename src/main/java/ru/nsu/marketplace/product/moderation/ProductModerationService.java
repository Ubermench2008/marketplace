package ru.nsu.marketplace.product.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.common.moderation.ReviewerResponse;
import ru.nsu.marketplace.product.ProductRepository;
import ru.nsu.marketplace.product.exception.ModerationRequestNotFoundException;
import ru.nsu.marketplace.product.exception.ProductModerationStateException;
import ru.nsu.marketplace.product.exception.ProductNotFoundException;
import ru.nsu.marketplace.product.model.ProductEntity;
import ru.nsu.marketplace.product.moderation.dto.ProductModerationResponse;
import ru.nsu.marketplace.product.moderation.dto.ProductModerationSnapshotResponse;
import ru.nsu.marketplace.user.UserEntity;
import ru.nsu.marketplace.user.UserRepository;
import ru.nsu.marketplace.user.exception.UserNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductModerationService {
    private final ProductModerationRepository productModerationRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductModerationResponse> getRequestsByStatus(ModerationStatus status, Pageable pageable) {
        return productModerationRepository.findAllByStatus(status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductModerationResponse getRequest(UUID requestId) {
        ProductModerationEntity request = productModerationRepository.findByPublicId(requestId).orElseThrow(
                () -> new ModerationRequestNotFoundException("Moderation request not found")
        );

        return toResponse(request);
    }

    @Transactional
    public void approveRequest(UUID requestId, Long moderatorId) {
        reviewRequest(requestId, moderatorId, ModerationStatus.APPROVED, null);
    }

    @Transactional
    public void rejectRequest(UUID requestId, Long moderatorId, String reason) {
        reviewRequest(requestId, moderatorId, ModerationStatus.REJECTED, reason);
    }

    private void reviewRequest(
            UUID requestId,
            Long moderatorId,
            ModerationStatus status,
            String rejectionReason
    ) {
        ProductModerationEntity moderationRequest = productModerationRepository.findByPublicIdForUpdate(requestId).orElseThrow(
                () -> new ModerationRequestNotFoundException("Moderation request not found")
        );

        ensureCanReviewModerationRequest(moderationRequest);

        ProductEntity product = productRepository.findByIdForUpdate(moderationRequest.getProduct().getId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        moderationRequest.setStatus(status);
        moderationRequest.setReviewedAt(Instant.now());
        moderationRequest.setReviewedBy(userRepository.findById(moderatorId).orElseThrow(
                () -> new UserNotFoundException("Moderator not found"))
        );

        moderationRequest.setRejectionReason(rejectionReason);

        product.setModerationStatus(status);
    }

    private void ensureCanReviewModerationRequest(ProductModerationEntity moderationRequest) {
        if (moderationRequest.getStatus() != ModerationStatus.PENDING) {
            throw new ProductModerationStateException("Only pending moderation requests can be reviewed");
        }
    }

    private ProductModerationResponse toResponse(ProductModerationEntity entity) {
        ProductModerationSnapshot snapshot = entity.getSnapshot();
        ProductModerationSnapshotResponse snapshotResponse = new ProductModerationSnapshotResponse(
                snapshot.getName(),
                snapshot.getSlug(),
                snapshot.getDescription(),
                snapshot.getPrice(),
                snapshot.getPreviewImageUrl(),
                List.copyOf(entity.getSnapshotImageUrls())
        );

        UserEntity reviewer = entity.getReviewedBy();
        ReviewerResponse reviewerResponse = reviewer == null ? null : new ReviewerResponse(
                reviewer.getName(), Set.copyOf(reviewer.getRoles())
        );

        return ProductModerationResponse.builder()
                .publicId(entity.getPublicId())
                .productId(entity.getProduct().getPublicId())
                .status(entity.getStatus())
                .submittedAt(entity.getSubmittedAt())
                .reviewedAt(entity.getReviewedAt())
                .reviewedBy(reviewerResponse)
                .rejectionReason(entity.getRejectionReason())
                .snapshot(snapshotResponse)
                .build();
    }
}
