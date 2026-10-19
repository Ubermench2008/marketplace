package ru.nsu.marketplace.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.nsu.marketplace.entity.OrderEntity;
import ru.nsu.marketplace.enums.OrderStatus;

import java.util.Collection;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Page<OrderEntity> findAllByUser_IdAndStatusIn(Long userId, Collection<OrderStatus> statuses, Pageable pageable);

    Page<OrderEntity> findAllByUser_Id(Long userId, Pageable pageable);

    Optional<OrderEntity> findByIdAndUser_Id(Long orderId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select orders from OrderEntity orders where orders.id = :orderId and orders.user.id = :userId")
    Optional<OrderEntity> findUserOrderForUpdate(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select orders from OrderEntity orders where orders.id = :orderId")
    Optional<OrderEntity> findOrderForUpdate(@Param("orderId") Long orderId);
}
