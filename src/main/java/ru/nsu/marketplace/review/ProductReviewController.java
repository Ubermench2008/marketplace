package ru.nsu.marketplace.review;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.nsu.marketplace.review.dto.CreateProductReviewRequest;
import ru.nsu.marketplace.review.dto.MyProductReviewResponse;
import ru.nsu.marketplace.review.dto.ProductReviewResponse;
import ru.nsu.marketplace.review.reply.dto.ProductReviewReplyResponse;
import ru.nsu.marketplace.review.reply.dto.CreateProductReviewReplyRequest;
import ru.nsu.marketplace.review.dto.UpdateProductReviewRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductReviewController {
    private final ProductReviewService reviewService;

    @GetMapping("/products/{productId}/reviews")
    public Page<ProductReviewResponse> getProductReviews(
            @PathVariable UUID productId,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return reviewService.getProductReviews(productId, pageable);
    }

    @GetMapping("/products/{productId}/reviews/my")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    public MyProductReviewResponse getMyProductReview(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Long userId
    ) {
        return reviewService.getMyProductReview(productId, userId);
    }

    @GetMapping("/reviews/{reviewId}/replies")
    public Page<ProductReviewReplyResponse> getProductReviewReplies(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 30) Pageable pageable
    ) {
        return reviewService.getProductReviewReplies(reviewId, pageable, userId);
    }

    @PostMapping("/reviews/{reviewId}/replies")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER') and !hasRole('ADMIN')")
    public ResponseEntity<Void> createProductReviewReply(
            @PathVariable Long reviewId,
            @RequestBody @Valid CreateProductReviewReplyRequest reply,
            @AuthenticationPrincipal Long userId
    ) {
        reviewService.createProductReviewReply(reviewId, reply, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/reviews/{reviewId}/replies/children")
    public Page<ProductReviewReplyResponse> getDirectReplies(
        @PathVariable Long reviewId,
        @RequestParam(required = false) Long parentId,
        @AuthenticationPrincipal Long userId,
        Pageable pageable
    ) {
        return reviewService.getDirectReplies(reviewId, parentId, pageable, userId);
    }

    @PostMapping("/products/{productId}/reviews")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    public ResponseEntity<MyProductReviewResponse> createProductReview(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid CreateProductReviewRequest request
    ) {
        MyProductReviewResponse response = reviewService.createProductReview(productId, userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/reviews/{reviewId}")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    public MyProductReviewResponse updateProductReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid UpdateProductReviewRequest request
    ) {
        return reviewService.updateProductReview(reviewId, userId, request);
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    public ResponseEntity<Void> deleteProductReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Long userId
    ) {
        reviewService.deleteProductReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }
}
