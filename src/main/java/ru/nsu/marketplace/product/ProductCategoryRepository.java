package ru.nsu.marketplace.product;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.marketplace.product.model.ProductCategoryEntity;

import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategoryEntity, Long> {
    Optional<ProductCategoryEntity> findBySlug(String slug);

    List<ProductCategoryEntity> findAllByOrderByNameAsc();
}
