package ru.nsu.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.marketplace.entity.ProductEntity;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findByPublicId(UUID publicId);
}
