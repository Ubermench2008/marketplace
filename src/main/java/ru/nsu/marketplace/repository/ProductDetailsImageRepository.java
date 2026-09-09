package ru.nsu.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.marketplace.entity.ProductDetailsImageEntity;

public interface ProductDetailsImageRepository extends JpaRepository<ProductDetailsImageEntity, Long> {}
