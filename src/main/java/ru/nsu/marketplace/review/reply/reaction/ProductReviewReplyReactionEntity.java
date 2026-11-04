package ru.nsu.marketplace.review.reply.reaction;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.nsu.marketplace.review.reaction.ReactionType;
import ru.nsu.marketplace.review.reply.ProductReviewReplyEntity;
import ru.nsu.marketplace.user.UserEntity;

@Entity
@Getter
@Setter
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_reply_reaction_reply_user", columnNames = {"reply_id", "user_id"}
))
public class ProductReviewReplyReactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reply_reaction_generator")
    @SequenceGenerator(
            name = "reply_reaction_generator",
            sequenceName = "product_review_reply_reaction_seq",
            allocationSize = 50
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reply_id", nullable = false)
    private ProductReviewReplyEntity reply;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReactionType type;
}
