package ru.nsu.marketplace.review.reaction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class ProductReviewReactionController {
    private final ProductReviewReactionService service;

    @GetMapping("/api/reviews/reactions/my")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER') and !hasRole('ADMIN')")
    public List<MyReviewReactionResponse> getMyReactions(
            @AuthenticationPrincipal Long userId,
            @RequestParam List<Long> reviewIds
    ) {
        return service.getMyReactions(userId, reviewIds);
    }

    @PutMapping("/api/reviews/{reviewId}/reaction")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER') and !hasRole('ADMIN')")
    public ResponseEntity<Void> setReaction(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid SetReactionRequest request
    ) {
        service.setReaction(reviewId, userId, request.type());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/reviews/{reviewId}/reaction")
    @PreAuthorize("hasAnyRole('BUYER', 'SELLER') and !hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReaction(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Long userId
    ) {
        service.deleteReaction(reviewId, userId);
        return ResponseEntity.noContent().build();
    }
}
