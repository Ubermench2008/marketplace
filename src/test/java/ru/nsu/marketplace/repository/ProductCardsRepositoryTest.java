package ru.nsu.marketplace.repository;


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
import ru.nsu.marketplace.entity.ProductCardEntity;
import ru.nsu.marketplace.entity.ProductEntity;

import java.math.BigDecimal;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.nsu.marketplace.StaticMethods.getTestEntity;

@DataJpaTest
@Testcontainers
public class ProductCardsRepositoryTest {

    @Autowired
    private ProductCardsRepository repository;

    @Autowired
    private ProductRepository productRepository;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");

    @Test
    void shouldSaveAndFindProductCardByPublicId() {
        UUID uuid = UUID.randomUUID();

        ProductCardEntity entity = new ProductCardEntity();
        entity.setPublicId(uuid);
        entity.setImgUrl("/media/products/iphone.webp");

        ProductEntity product = new ProductEntity();
        product.setPublicId(UUID.randomUUID());
        product.setSlug("iphone-16-pro");
        product.setName("iPhone 16 Pro");
        product.setPrice(new BigDecimal("99990.00"));
        product.setDescription("A sufficiently long product description for tests.");
        entity.setProduct(productRepository.save(product));

        repository.save(entity);

        ProductCardEntity result = repository.findByPublicId(uuid).orElseThrow();

        assertThat(result.getPublicId()).isEqualTo(uuid);
        assertThat(result.getProduct().getSlug()).isEqualTo("iphone-16-pro");
        assertThat(result.getProduct().getName()).isEqualTo("iPhone 16 Pro");
        assertThat(result.getProduct().getPrice())
                .isEqualByComparingTo("99990.00");
    }

    @Test
    void shouldReturnCorrectProductCardPage() {
        List<ProductCardEntity> testEntities = new ArrayList<>();

        for (int i = 0; i < 10; i++) { testEntities.add(getTestEntity(UUID.randomUUID())); }

        testEntities.forEach(entity -> {
            entity.setProduct(productRepository.save(entity.getProduct()));
            repository.save(entity);
        });

        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "name"));

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
}
