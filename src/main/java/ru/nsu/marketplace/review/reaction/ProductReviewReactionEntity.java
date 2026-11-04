package ru.nsu.marketplace.review.reaction;

import jakarta.persistence.*;
import lombok.*;
import ru.nsu.marketplace.review.ProductReviewEntity;
import ru.nsu.marketplace.user.UserEntity;

@Entity
@Setter
@Getter
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_review_reaction_review_user",
                        columnNames = {"review_id", "user_id"})
        }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewReactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_review_reaction_generator")
    @SequenceGenerator(
            name = "product_review_reaction_generator",
            sequenceName = "product_review_reaction_seq",
            allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private ProductReviewEntity review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReactionType type;
}
