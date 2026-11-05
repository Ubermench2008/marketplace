package ru.nsu.marketplace.review.moderation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.review.moderation.dto.ApproveReviewModerationRequest;
import ru.nsu.marketplace.review.moderation.dto.ProductReviewModerationResponse;
import ru.nsu.marketplace.review.moderation.dto.RejectReviewModerationRequest;
import ru.nsu.marketplace.review.moderation.dto.ReviewReplyModerationResponse;
import ru.nsu.marketplace.review.reply.ReplyStatus;
import ru.nsu.marketplace.review.reply.dto.ReviewReplyStatusUpdateRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductReviewAndReplyModerationController {
    private final ProductReviewModerationService reviewModerationService;
    private final ProductReviewAndReplyModerationService reviewAndReplyModerationService;

    @GetMapping("/review-requests")
    public Page<ProductReviewModerationResponse> getRequestsByStatus(
            @RequestParam(name = "status", defaultValue = "PENDING") ModerationStatus status,
            @PageableDefault(size = 20, sort = {"submittedAt", "id"}) Pageable pageable
    ) {
        return reviewModerationService.getRequestsByStatus(status, pageable);
    }

    @GetMapping("/review-requests/{requestId}")
    public ProductReviewModerationResponse getReviewRequest(@PathVariable UUID requestId) {
        return reviewModerationService.getRequest(requestId);
    }

    @PostMapping("/review-requests/{requestId}/approve")
    public ResponseEntity<Void> approveReviewRequest(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal Long moderatorId,
            @RequestBody @Valid ApproveReviewModerationRequest request
    ) {
        reviewModerationService.approveRequest(requestId, moderatorId, request.reviewVersion());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/review-requests/{requestId}/reject")
    public ResponseEntity<Void> rejectReviewRequest(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal Long moderatorId,
            @RequestBody @Valid RejectReviewModerationRequest request
    ) {
        reviewModerationService.rejectRequest(requestId, moderatorId, request.reviewVersion(), request.reason());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/review-replies/{reviewReplyId}")
    public ResponseEntity<Void> updateReviewReplyStatus(
            @PathVariable Long reviewReplyId,
            @RequestBody ReviewReplyStatusUpdateRequest request
    ) {
        reviewAndReplyModerationService.updateReviewReplyStatus(reviewReplyId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/review-replies")
    public Page<ReviewReplyModerationResponse> getReviewRepliesForModeration(
            @RequestParam("status") ReplyStatus replyStatus,
            Pageable pageable
    ) {
        return reviewAndReplyModerationService.getReviewRepliesForModeration(replyStatus, pageable);
    }
}
