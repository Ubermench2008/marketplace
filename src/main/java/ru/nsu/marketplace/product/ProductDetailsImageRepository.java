package ru.nsu.marketplace.product;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.marketplace.product.model.ProductDetailsImageEntity;

public interface ProductDetailsImageRepository extends JpaRepository<ProductDetailsImageEntity, Long> {}
