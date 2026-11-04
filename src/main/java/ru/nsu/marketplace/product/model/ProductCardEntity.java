package ru.nsu.marketplace.product.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "product_cards")
public class ProductCardEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_card_id_generator")
    @SequenceGenerator(
            name = "product_card_id_generator",
            sequenceName = "product_cards_seq",
            allocationSize = 50
    )
    private Long id;

    @Column(name = "img_url")
    private String previewImageUrl;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            unique = true
    )
    private ProductEntity product;
}
