package ru.nsu.marketplace.product.stock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.nsu.marketplace.product.model.ProductStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductStockRepository extends JpaRepository<ProductStockEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stock from ProductStockEntity stock where stock.productId = :productId")
    Optional<ProductStockEntity> findByProductIdForUpdate(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select stock from ProductStockEntity stock
            where stock.productId = (
                select product.id from ProductEntity product
                where product.publicId = :productId and product.seller.id = :sellerId
            )
            """)
    Optional<ProductStockEntity> findSellerStockForUpdate(
            @Param("productId") UUID productId,
            @Param("sellerId") Long sellerId
    );

    @Query("""
            select new ru.nsu.marketplace.product.stock.ProductStockResponse(product.publicId, stock.quantity)
            from ProductStockEntity stock
            join stock.product product
            where product.publicId in :productIds and product.status = :status
            """)
    List<ProductStockResponse> findStocksByProductPublicIdsAndStatus(
            @Param("productIds") List<UUID> productIds,
            @Param("status") ProductStatus status
    );

    @Query("""
            select new ru.nsu.marketplace.product.stock.ProductStockResponse(product.publicId, stock.quantity)
            from ProductStockEntity stock
            join stock.product product
            where product.publicId = :productId and product.seller.id = :sellerId
            """)
    Optional<ProductStockResponse> findStockByProductPublicIdAndSellerId(
            @Param("productId") UUID productId,
            @Param("sellerId") Long sellerId
    );
}
