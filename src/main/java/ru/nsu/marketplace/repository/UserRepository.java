package ru.nsu.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.marketplace.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByTelephoneNumber(String telephoneNumber);
    boolean existsByEmail(String email);

    Optional<UserEntity> findByTelephoneNumber(String telephoneNumber);

    Optional<UserEntity> findByEmail(String email);
}
