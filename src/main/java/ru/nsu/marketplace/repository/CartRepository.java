package ru.nsu.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.marketplace.entity.CartEntity;

import java.util.Optional;

public interface CartRepository extends JpaRepository<CartEntity, Long> {
    Optional<CartEntity> findByUser_Id(Long userId);
}
