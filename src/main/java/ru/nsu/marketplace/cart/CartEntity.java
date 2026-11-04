package ru.nsu.marketplace.cart;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.nsu.marketplace.user.UserEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_carts")
@Getter
@Setter
public class CartEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cart_id_generator")
    @SequenceGenerator(
            name = "cart_id_generator",
            sequenceName = "user_carts_seq",
            allocationSize = 50
    )
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            unique = true,
            nullable = false
    )
    private UserEntity user;

    @OneToMany(
            mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<CartItemEntity> items = new ArrayList<>();
}
