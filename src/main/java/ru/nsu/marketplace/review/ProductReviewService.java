package ru.nsu.marketplace.review;

import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.product.ProductRepository;
import ru.nsu.marketplace.product.exception.ProductNotFoundException;
import ru.nsu.marketplace.product.model.ProductEntity;
import ru.nsu.marketplace.review.dto.CreateProductReviewRequest;
import ru.nsu.marketplace.review.dto.MyProductReviewResponse;
import ru.nsu.marketplace.review.dto.ProductReviewResponse;
import ru.nsu.marketplace.review.reply.dto.ProductReviewReplyResponse;
import ru.nsu.marketplace.review.reply.dto.CreateProductReviewReplyRequest;
import ru.nsu.marketplace.review.dto.UpdateProductReviewRequest;
import ru.nsu.marketplace.review.exception.ProductReviewNotFoundException;
import ru.nsu.marketplace.review.exception.ProductReviewStateException;
import ru.nsu.marketplace.review.exception.ReviewReplyNotFoundException;
import ru.nsu.marketplace.review.moderation.ProductReviewModerationEntity;
import ru.nsu.marketplace.review.moderation.ProductReviewModerationRepository;
import ru.nsu.marketplace.review.reaction.ProductReviewReactionRepository;
import ru.nsu.marketplace.review.reaction.ReactionType;
import ru.nsu.marketplace.review.reaction.ReviewReactionCounts;
import ru.nsu.marketplace.review.reaction.ReviewReactionCountsByReview;
import ru.nsu.marketplace.review.reply.ProductReviewReplyEntity;
import ru.nsu.marketplace.review.reply.ProductReviewReplyRepository;
import ru.nsu.marketplace.review.reply.ReplyStatus;
import ru.nsu.marketplace.review.reply.reaction.ProductReviewReplyReactionRepository;
import ru.nsu.marketplace.review.reply.reaction.ReplyReactionCounts;
import ru.nsu.marketplace.user.UserEntity;
import ru.nsu.marketplace.user.UserRepository;
import ru.nsu.marketplace.user.Role;
import ru.nsu.marketplace.user.exception.UserNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductReviewService {
    private final ProductReviewRepository reviewRepository;
    private final ProductReviewModerationRepository reviewModerationRepository;
    private final ProductReviewReactionRepository productReviewReactionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductReviewReplyRepository reviewReplyRepository;
    private final ProductReviewReplyReactionRepository replyReactionRepository;

    @Transactional(readOnly = true)
    public Page<ProductReviewReplyResponse> getProductReviewReplies(
            Long reviewId,
            Pageable pageable,
            Long userId
    ) {
        ProductReviewEntity review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ProductReviewNotFoundException("Review not found")
        );
        ensureReviewNotDeleted(review);
        if (review.getModerationStatus() != ModerationStatus.APPROVED) {
            throw new ProductReviewNotFoundException("Review not found");
        }
        Pageable likesOrderedPage = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<ProductReviewReplyEntity> replies = reviewReplyRepository.findByReviewIdAndStatusOrderByLikes(
                reviewId,
                ReplyStatus.PUBLISHED,
                ReactionType.LIKE,
                likesOrderedPage
        );

        if (replies.isEmpty()) {
            return replies.map(reply -> toReplyResponse(reply, 0, 0, null));
        }
        List<Long> replyIds = replies.getContent().stream()
                .map(ProductReviewReplyEntity::getId)
                .toList();

        Map<Long, ReplyReactionCounts> countsByReplyId = replyReactionRepository
                .countByReplyIds(replyIds, ReactionType.LIKE, ReactionType.DISLIKE)
                .stream()
                .collect(Collectors.toMap(
                        ReplyReactionCounts::getReplyId,
                        Function.identity()
                ));

        Map<Long, ReactionType> ownReactionsByReplyId;

        if (userId == null) {
            ownReactionsByReplyId = Map.of();
        } else {
            ownReactionsByReplyId = replyReactionRepository
                    .findAllByReply_IdInAndUser_Id(replyIds, userId)
                    .stream()
                    .collect(Collectors.toMap(
                            reaction -> reaction.getReply().getId(),
                            reaction -> reaction.getType()
                    ));
        }

        return replies.map(reply -> {
            ReplyReactionCounts counts = countsByReplyId.get(reply.getId());
            long likes = counts == null ? 0 : counts.getLikes();
            long dislikes = counts == null ? 0 : counts.getDislikes();
            ReactionType ownReaction = ownReactionsByReplyId.get(reply.getId());

            return toReplyResponse(reply, likes, dislikes, ownReaction);
        });
    }

    @Transactional
    public void createProductReviewReply(Long reviewId, CreateProductReviewReplyRequest request, Long userId) {
        ensureReplyTextValid(request.text());

        ProductReviewEntity review = reviewRepository.findByIdForUpdate(reviewId).orElseThrow(
                () -> new ProductReviewNotFoundException("Review not found")
        );
        ensureReviewNotDeleted(review);
        if (review.getModerationStatus() != ModerationStatus.APPROVED) {
            throw new ProductReviewNotFoundException("Review not found");
        }

        UserEntity author = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );
        ensureUserCanReply(author);

        ProductReviewReplyEntity replyTo = null;
        if (request.replyToId() != null) {
            replyTo = reviewReplyRepository.findById(request.replyToId()).orElseThrow(
                    () -> new ReviewReplyNotFoundException("Review reply not found")
            );
            ensureReplyTargetValid(replyTo, reviewId);
        }

        ProductReviewReplyEntity reply = ProductReviewReplyEntity.builder()
                .review(review)
                .author(author)
                .replyTo(replyTo)
                .isSeller(Objects.equals(review.getProduct().getSeller().getId(), userId))
                .text(request.text())
                .status(ReplyStatus.PUBLISHED)
                .build();
        reviewReplyRepository.save(reply);
    }

    private void ensureReplyTextValid(String text) {
        if (text == null || text.isBlank() || text.length() > 500) {
            throw new IllegalArgumentException("Reply text must be non-blank and contain at most 500 characters");
        }
    }

    private void ensureUserCanReply(UserEntity user) {
        if (user.getRoles().contains(Role.ADMIN)
                || (!user.getRoles().contains(Role.BUYER) && !user.getRoles().contains(Role.SELLER))) {
            throw new AccessDeniedException("Only buyers and sellers can reply to reviews");
        }
    }

    private void ensureReplyTargetValid(ProductReviewReplyEntity replyTo, Long reviewId) {
        if (!Objects.equals(replyTo.getReview().getId(), reviewId)) {
            throw new IllegalArgumentException("Reply target must belong to the same review");
        }
        if (replyTo.getStatus() != ReplyStatus.PUBLISHED) {
            throw new ReviewReplyNotFoundException("Review reply not found");
        }
    }

    private ProductReviewReplyResponse toReplyResponse(
            ProductReviewReplyEntity reply,
            long likes,
            long dislikes,
            ReactionType myReaction
    ) {
        return new ProductReviewReplyResponse(
                reply.getId(),
                reply.getAuthor().getId(),
                reply.getReview().getId(),
                reply.getAuthor().getName(),
                Boolean.TRUE.equals(reply.getIsSeller()),
                reply.getReplyTo() == null ? null : reply.getReplyTo().getId(),
                reply.getText(),
                reply.getCreatedAt(),
                likes,
                dislikes,
                myReaction
        );
    }

    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> getProductReviews(UUID productId, Pageable pageable) {
        Page<ProductReviewEntity> reviews = reviewRepository
                .findAllByProduct_PublicIdAndModerationStatusAndDeletedAtIsNull(
                        productId,
                        ModerationStatus.APPROVED,
                        pageable
                );

        if (reviews.isEmpty()) {
            return reviews.map(review -> toResponse(review, 0, 0));
        }

        List<Long> reviewIds = reviews.getContent().stream()
                .map(ProductReviewEntity::getId)
                .toList();
        Map<Long, ReviewReactionCountsByReview> countsByReviewId = productReviewReactionRepository
                .countReactionsByReviewIds(reviewIds, ReactionType.LIKE, ReactionType.DISLIKE)
                .stream()
                .collect(Collectors.toMap(
                        ReviewReactionCountsByReview::getReviewId,
                        Function.identity()
                ));

        return reviews.map(review -> {
            ReviewReactionCounts counts = countsByReviewId.get(review.getId());
            long likes = counts == null ? 0 : counts.getLikes();
            long dislikes = counts == null ? 0 : counts.getDislikes();

            return toResponse(review, likes, dislikes);
        });
    }

    @Transactional(readOnly = true)
    public MyProductReviewResponse getMyProductReview(UUID productId, Long userId) {
        ProductReviewEntity review = reviewRepository
                .findByProduct_PublicIdAndUser_IdAndDeletedAtIsNull(productId, userId)
                .orElseThrow(() -> new ProductReviewNotFoundException("Your review for this product was not found"));
        ProductReviewModerationEntity moderation = reviewModerationRepository
                .findFirstByProductReview_IdOrderBySubmittedAtDescIdDesc(review.getId()).orElseThrow(
                        () -> new ProductReviewNotFoundException("Review moderation request not found")
                );
        return toMyReviewResponse(moderation);
    }

    @Transactional
    public MyProductReviewResponse createProductReview(
            UUID productId,
            Long userId,
            CreateProductReviewRequest request
    ) {
        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );
        UserEntity user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );
        ensureReviewDoesNotExist(product.getId(), userId);

        ProductReviewEntity review = new ProductReviewEntity();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.rating());
        review.setText(request.text());
        try {
            reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException violation
                    && "uk_product_review_product_user".equals(violation.getConstraintName())) {
                throw new ProductReviewStateException("You have already reviewed this product");
            }
            throw exception;
        }

        ProductReviewModerationEntity moderation = new ProductReviewModerationEntity();
        moderation.setPublicId(UUID.randomUUID());
        moderation.setProductReview(review);
        moderation.setReviewRating(review.getRating());
        moderation.setReviewText(review.getText());
        reviewModerationRepository.save(moderation);
        return toMyReviewResponse(moderation);
    }

    @Transactional
    public MyProductReviewResponse updateProductReview(
            Long reviewId,
            Long userId,
            UpdateProductReviewRequest request
    ) {
        ProductReviewModerationEntity moderation = reviewModerationRepository
                .findLatestByReviewIdForUpdate(reviewId, PageRequest.of(0, 1)).stream().findFirst().orElseThrow(
                        () -> new ProductReviewNotFoundException("Review moderation request not found")
                );
        ProductReviewEntity review = reviewRepository.findByIdForUpdate(reviewId).orElseThrow(
                () -> new ProductReviewNotFoundException("Review not found")
        );
        ensureReviewNotDeleted(review);
        ensureReviewAuthor(review, userId);
        ensureModerationRequestIsPending(moderation);

        boolean changed = false;
        if (request.rating() != null && request.rating() != review.getRating()) {
            review.setRating(request.rating());
            changed = true;
        }
        if (request.text() != null) {
            String text = request.text().isEmpty() ? null : request.text();
            if (!Objects.equals(text, review.getText())) {
                review.setText(text);
                changed = true;
            }
        }
        if (changed) {
            moderation.setReviewRating(review.getRating());
            moderation.setReviewText(review.getText());
            moderation.setReviewVersion(moderation.getReviewVersion() + 1);
        }
        return toMyReviewResponse(moderation);
    }

    @Transactional
    public void deleteProductReview(Long reviewId, Long userId) {
        ProductReviewModerationEntity moderation = reviewModerationRepository
                .findLatestByReviewIdForUpdate(reviewId, PageRequest.of(0, 1)).stream().findFirst().orElseThrow(
                        () -> new ProductReviewNotFoundException("Review moderation request not found")
                );
        ProductReviewEntity review = reviewRepository.findByIdForUpdate(reviewId).orElseThrow(
                () -> new ProductReviewNotFoundException("Review not found")
        );
        ensureReviewAuthor(review, userId);
        if (review.getDeletedAt() != null) {
            return;
        }
        review.setDeletedAt(Instant.now());
        if (moderation.getStatus() == ModerationStatus.PENDING) {
            moderation.setStatus(ModerationStatus.CANCELLED);
            review.setModerationStatus(ModerationStatus.CANCELLED);
        }
    }

    private void ensureReviewNotDeleted(ProductReviewEntity review) {
        if (review.getDeletedAt() != null) {
            throw new ProductReviewNotFoundException("Review not found");
        }
    }

    private void ensureReviewDoesNotExist(Long productId, Long userId) {
        if (reviewRepository.existsByProduct_IdAndUser_IdAndDeletedAtIsNull(productId, userId)) {
            throw new ProductReviewStateException("You have already reviewed this product");
        }
    }

    private void ensureReviewAuthor(ProductReviewEntity review, Long userId) {
        if (userId == null || !Objects.equals(review.getUser().getId(), userId)) {
            throw new AccessDeniedException("Only the author can modify this review");
        }
    }

    private void ensureModerationRequestIsPending(ProductReviewModerationEntity moderation) {
        if (moderation.getStatus() != ModerationStatus.PENDING) {
            throw new ProductReviewStateException("Only reviews awaiting moderation can be edited");
        }
    }

    private MyProductReviewResponse toMyReviewResponse(ProductReviewModerationEntity moderation) {
        ProductReviewEntity review = moderation.getProductReview();
        ReviewReactionCounts counts = productReviewReactionRepository.countReactionsByReviewId(
                review.getId(),
                ReactionType.LIKE,
                ReactionType.DISLIKE
        );

        ProductReviewResponse response = new ProductReviewResponse(
                review.getId(),
                review.getProduct().getPublicId(),
                review.getUser().getId(),
                review.getUser().getName(),
                moderation.getReviewRating(),
                moderation.getReviewText(),
                counts.getLikes(),
                counts.getDislikes(),
                review.getCreatedAt()
        );
        return new MyProductReviewResponse(
                response,
                moderation.getStatus(),
                moderation.getReviewVersion(),
                moderation.getRejectionReason()
        );
    }

    private ProductReviewResponse toResponse(ProductReviewEntity review, long likes, long dislikes) {
        return new ProductReviewResponse(
                review.getId(),
                review.getProduct().getPublicId(),
                review.getUser().getId(),
                review.getUser().getName(),
                review.getRating(),
                review.getText(),
                likes,
                dislikes,
                review.getCreatedAt()
        );
    }
}
