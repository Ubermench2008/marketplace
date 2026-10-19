package ru.nsu.marketplace;

import ru.nsu.marketplace.entity.ProductCardEntity;
import ru.nsu.marketplace.entity.ProductEntity;
import ru.nsu.marketplace.entity.UserEntity;
import ru.nsu.marketplace.enums.Role;

import java.util.Set;

import java.math.BigDecimal;
import java.util.UUID;

public class StaticMethods {
    public static ProductCardEntity getTestProductCardEntity(UUID uuid) {
        ProductCardEntity entity = new ProductCardEntity();
        entity.setImgUrl("/media/products/iphone.webp");

        ProductEntity product = ProductEntity.builder()
                .publicId(uuid)
                .slug("iphone-16-pro")
                .name("iphone 16 pro")
                .price(new BigDecimal("99990.00"))
                .description("A sufficiently long product description for tests.")
                .seller(UserEntity.builder().id(10L).roles(Set.of(Role.SELLER)).build())
                .build();
        entity.setProduct(product);

        return entity;
    }

    public static ProductCardEntity getTestProductCardEntity(
            UUID publicId,
            String name,
            String slug,
            BigDecimal price,
            String imageUrl
    ) {
        ProductCardEntity entity = new ProductCardEntity();
        entity.setImgUrl(imageUrl);

        ProductEntity product = ProductEntity.builder()
                .publicId(publicId)
                .slug(slug)
                .name(name)
                .price(price)
                .description("A sufficiently long product description for tests.")
                .seller(UserEntity.builder().id(10L).roles(Set.of(Role.SELLER)).build())
                .build();
        entity.setProduct(product);

        return entity;
    }
}
