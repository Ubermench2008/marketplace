package ru.nsu.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.marketplace.entity.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
}
