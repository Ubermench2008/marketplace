package ru.nsu.marketplace;

import ru.nsu.marketplace.entity.ProductCardEntity;
import ru.nsu.marketplace.entity.ProductEntity;

import java.math.BigDecimal;
import java.util.UUID;

public class StaticMethods {
    public static ProductCardEntity getTestProductCardEntity(UUID uuid){
        ProductCardEntity entity = new ProductCardEntity();
        entity.setImgUrl("/media/products/iphone.webp");

        ProductEntity product = new ProductEntity();
        product.setPublicId(uuid);
        product.setSlug("iphone-16-pro");
        product.setName("iphone 16 pro");
        product.setPrice(new BigDecimal("99990.00"));
        product.setDescription("A sufficiently long product description for tests.");
        entity.setProduct(product);

        return entity;
    }

    public static ProductCardEntity getTestProductCardEntity(UUID publicId, String name, String slug, BigDecimal price, String imageUrl){
        ProductCardEntity entity = new ProductCardEntity();
        entity.setImgUrl(imageUrl);

        ProductEntity product = new ProductEntity();
        product.setPublicId(publicId);
        product.setSlug(slug);
        product.setName(name);
        product.setPrice(price);
        product.setDescription("A sufficiently long product description for tests.");
        entity.setProduct(product);

        return entity;
    }
}
