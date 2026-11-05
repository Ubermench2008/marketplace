package ru.nsu.marketplace.product.stock;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.nsu.marketplace.product.model.ProductEntity;

@Entity
@Table(name = "product_stocks")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductStockEntity {
    @Id
    @Column(name = "product_id")
    private Long productId;

    @MapsId
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @PositiveOrZero
    @Column(nullable = false)
    private int quantity;
}
