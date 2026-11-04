package ru.nsu.marketplace.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.nsu.marketplace.product.model.ProductCardEntity;
import ru.nsu.marketplace.product.model.ProductEntity;
import ru.nsu.marketplace.user.Role;
import ru.nsu.marketplace.user.UserEntity;
import ru.nsu.marketplace.user.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
public class ProductCardRepositoryTest {

    @Autowired
    private ProductCardRepository repository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Test
    void shouldSaveProductCard() {
        UUID uuid = UUID.randomUUID();

        ProductCardEntity entity = new ProductCardEntity();
        entity.setPreviewImageUrl("/media/products/iphone.webp");

        ProductEntity product = ProductEntity.builder()
            .publicId(uuid)
            .slug("iphone-16-pro")
            .name("iPhone 16 Pro")
            .price(new BigDecimal("99990.00"))
            .description("A sufficiently long product description for tests.")
            .seller(createSeller())
            .build();
        entity.setProduct(productRepository.save(product));

        ProductCardEntity saved = repository.save(entity);

        ProductCardEntity result = repository.findById(saved.getId()).orElseThrow();

        assertThat(result.getProduct().getPublicId()).isEqualTo(uuid);
        assertThat(result.getProduct().getSlug()).isEqualTo("iphone-16-pro");
        assertThat(result.getProduct().getName()).isEqualTo("iPhone 16 Pro");
        assertThat(result.getProduct().getPrice()).isEqualByComparingTo("99990.00");
    }

    @Test
    void shouldReturnCorrectProductCardPage() {
        List<ProductCardEntity> testEntities = new ArrayList<>();
        UserEntity seller = createSeller();

        for (int i = 0; i < 10; i++) {
            ProductCardEntity entity = ProductTestFixtures.createProductCard(UUID.randomUUID());
            entity.getProduct().setSlug("iphone-16-pro-" + i);
            entity.getProduct().setSeller(seller);
            testEntities.add(entity);
        }

        testEntities.forEach(entity -> {
            entity.setProduct(productRepository.save(entity.getProduct()));
            repository.save(entity);
        });

        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "product.name"));

        Page<ProductCardEntity> result = repository.findAll(pageable);

        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getTotalElements()).isEqualTo(10);

        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isTrue();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();

        assertThat(result.getContent().getFirst().getProduct().getName()).isEqualTo("iphone 16 pro");
    }

    private UserEntity createSeller() {
        return userRepository.save(
            UserEntity.builder()
                .name("test_seller")
                .telephoneNumber("+79991234570")
                .passwordHash("test-password-hash")
                .roles(Set.of(Role.SELLER))
                .build()
        );
    }
}
