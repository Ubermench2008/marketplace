package ru.nsu.marketplace.review.reply.reaction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import ru.nsu.marketplace.review.reaction.SetReactionRequest;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/review-replies/{replyId}/reaction")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER', 'SELLER') and !hasRole('ADMIN')")
public class ProductReviewReplyReactionController {
    private final ProductReviewReplyReactionService service;

    @PutMapping
    public ResponseEntity<Void> setReaction(
            @PathVariable Long replyId,
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid SetReactionRequest request
    ) {
        service.setReaction(replyId, userId, request.type());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteReaction(
            @PathVariable Long replyId,
            @AuthenticationPrincipal Long userId
    ) {
        service.deleteReaction(replyId, userId);
        return ResponseEntity.noContent().build();
    }
}
