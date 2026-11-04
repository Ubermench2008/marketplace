package ru.nsu.marketplace.product;

import ru.nsu.marketplace.product.model.ProductCardEntity;
import ru.nsu.marketplace.product.model.ProductEntity;
import ru.nsu.marketplace.user.Role;
import ru.nsu.marketplace.user.UserEntity;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public final class ProductTestFixtures {

    private ProductTestFixtures() {}

    public static ProductCardEntity createProductCard(UUID uuid) {
        ProductCardEntity entity = new ProductCardEntity();
        entity.setPreviewImageUrl("/media/products/iphone.webp");

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

    public static ProductCardEntity createProductCard(
        UUID publicId,
        String name,
        String slug,
        BigDecimal price,
        String imageUrl
    ) {
        ProductCardEntity entity = new ProductCardEntity();
        entity.setPreviewImageUrl(imageUrl);

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
