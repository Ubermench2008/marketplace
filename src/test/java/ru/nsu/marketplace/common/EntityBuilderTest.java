package ru.nsu.marketplace.common;

import org.junit.jupiter.api.Test;
import ru.nsu.marketplace.product.model.ProductDetailsImageEntity;
import ru.nsu.marketplace.product.model.ProductEntity;
import ru.nsu.marketplace.user.Role;
import ru.nsu.marketplace.user.UserEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityBuilderTest {

    @Test
    void shouldCreateUsersWithIndependentEmptyRoles() {
        UserEntity firstUser = UserEntity.builder().build();
        UserEntity secondUser = UserEntity.builder().build();

        assertThat(firstUser.getRoles()).isEmpty();
        assertThat(secondUser.getRoles()).isEmpty();

        firstUser.getRoles().add(Role.BUYER);

        assertThat(firstUser.getRoles()).containsExactly(Role.BUYER);
        assertThat(secondUser.getRoles()).isEmpty();
    }

    @Test
    void shouldCreateProductsWithIndependentEmptyDetailsImages() {
        ProductEntity firstProduct = ProductEntity.builder().build();
        ProductEntity secondProduct = ProductEntity.builder().build();
        ProductDetailsImageEntity image = ProductDetailsImageEntity.builder()
            .product(firstProduct)
            .imageUrl("/media/image.webp")
            .position(0)
            .build();

        assertThat(firstProduct.getDetailsImages()).isEmpty();
        assertThat(secondProduct.getDetailsImages()).isEmpty();

        firstProduct.getDetailsImages().add(image);

        assertThat(firstProduct.getDetailsImages()).containsExactly(image);
        assertThat(secondProduct.getDetailsImages()).isEmpty();
    }

    @Test
    void shouldInitializeCollectionsWhenUsingNoArgsConstructors() {
        UserEntity user = new UserEntity();
        ProductEntity product = new ProductEntity();
        ProductDetailsImageEntity image = new ProductDetailsImageEntity();

        assertThat(user.getRoles()).isEmpty();
        assertThat(product.getDetailsImages()).isEmpty();
        assertThat(image.getPosition()).isZero();
    }
}
