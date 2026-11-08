package ru.nsu.marketplace.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.product.model.ProductCardEntity;
import ru.nsu.marketplace.product.model.ProductStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductCardRepository extends JpaRepository<ProductCardEntity, Long> {
    Optional<ProductCardEntity> findByProduct_PublicId(UUID productPublicId);

    @Query("""
            select card
            from ProductCardEntity card
            left join card.product.category category
            where card.product.status = :status
              and card.product.moderationStatus = :moderationStatus
              and (:categorySlug is null or category.slug = :categorySlug)
              and (:minPrice is null or card.product.price >= :minPrice)
              and (:maxPrice is null or card.product.price <= :maxPrice)
              and exists (
                  select stock.productId
                  from ProductStockEntity stock
                  where stock.productId = card.product.id
                    and stock.quantity > 0
            )
            """)
    Page<ProductCardEntity> findAllByProduct_StatusAndProduct_ModerationStatus(
            @Param("status") ProductStatus status,
            @Param("moderationStatus") ModerationStatus moderationStatus,
            @Param("categorySlug") String categorySlug,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    Page<ProductCardEntity> findAllByProduct_Seller_Id(
            Long sellerId,
            Pageable pageable
    );
}
