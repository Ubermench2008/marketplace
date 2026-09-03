package ru.nsu.marketplace.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.nsu.marketplace.entity.ProductCardEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductCardsRepository extends JpaRepository<ProductCardEntity, Long> {
    Optional<ProductCardEntity> findByPublicId(UUID publicID);
    Page<ProductCardEntity> findAll(Pageable pageable);
}
