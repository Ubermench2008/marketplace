package ru.nsu.marketplace.product.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "product_categories")
public class ProductCategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_category_id_generator")
    @SequenceGenerator(
        name = "product_category_id_generator",
        sequenceName = "product_categories_seq",
        allocationSize = 50
    )
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;
}
