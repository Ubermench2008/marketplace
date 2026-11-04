package ru.nsu.marketplace.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nsu.marketplace.product.model.ProductCardEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductCardRepository extends JpaRepository<ProductCardEntity, Long> {
    Optional<ProductCardEntity> findByProduct_PublicId(UUID productPublicId);
    Page<ProductCardEntity> findAllByProduct_Seller_Id(
            Long sellerId,
            Pageable pageable
    );
}
