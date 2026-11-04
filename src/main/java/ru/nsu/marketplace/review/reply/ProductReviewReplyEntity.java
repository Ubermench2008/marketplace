package ru.nsu.marketplace.review.reply;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import ru.nsu.marketplace.review.ProductReviewEntity;
import ru.nsu.marketplace.user.UserEntity;

import java.time.Instant;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewReplyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_review_reply_id_generator")
    @SequenceGenerator(
            name = "product_review_reply_id_generator",
            sequenceName = "product_review_reply",
            allocationSize = 50
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private ProductReviewEntity review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to")
    private ProductReviewReplyEntity replyTo;

    @Column(name = "is_seller", nullable = false, updatable = false)
    @Builder.Default
    private Boolean isSeller = false;

    @NotBlank
    @Length(max = 500)
    @Column(length = 500)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReplyStatus status;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
