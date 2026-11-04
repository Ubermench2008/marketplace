package ru.nsu.marketplace.product.moderation;

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
import ru.nsu.marketplace.product.moderation.dto.ProductModerationResponse;
import ru.nsu.marketplace.product.moderation.dto.RejectProductModerationRequest;

import java.util.UUID;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
public class ProductModerationController {
    private final ProductModerationService moderationService;

    @GetMapping("/product-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ProductModerationResponse> getRequestsByStatus(
            @RequestParam(name = "status", defaultValue = "PENDING") ModerationStatus moderationStatus,
            @PageableDefault(size = 20, sort = {"submittedAt", "id"}) Pageable pageable
    ) {
        return moderationService.getRequestsByStatus(moderationStatus, pageable);
    }

    @GetMapping("/product-requests/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductModerationResponse getRequest(@PathVariable UUID requestId) {
        return moderationService.getRequest(requestId);
    }

    @PostMapping("/product-requests/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveRequest(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal Long moderatorId
    ) {
        moderationService.approveRequest(requestId, moderatorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/product-requests/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rejectRequest(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal Long moderatorId,
            @RequestBody @Valid RejectProductModerationRequest request
    ) {
        moderationService.rejectRequest(requestId, moderatorId, request.reason());
        return ResponseEntity.noContent().build();
    }
}
