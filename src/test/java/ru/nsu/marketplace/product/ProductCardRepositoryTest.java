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
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.product.model.ProductCardEntity;
import ru.nsu.marketplace.product.model.ProductEntity;
import ru.nsu.marketplace.product.model.ProductStatus;
import ru.nsu.marketplace.product.stock.ProductStockEntity;
import ru.nsu.marketplace.product.stock.ProductStockRepository;
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
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ProductStockRepository stockRepository;

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
            .category(productCategoryRepository.findBySlug("electronics").orElseThrow())
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
    void shouldPaginateOnlyApprovedProductsForSaleWithPositiveStock() {
        List<ProductCardEntity> testEntities = new ArrayList<>();
        UserEntity seller = createSeller();

        for (int i = 0; i < 10; i++) {
            ProductCardEntity entity = ProductTestFixtures.createProductCard(UUID.randomUUID());
            entity.getProduct().setSlug("iphone-16-pro-" + i);
            entity.getProduct().setSeller(seller);
            entity.getProduct().setCategory(productCategoryRepository.findBySlug("electronics").orElseThrow());
            entity.getProduct().setStatus(ProductStatus.ACTIVE);
            entity.getProduct().setModerationStatus(ModerationStatus.APPROVED);
            testEntities.add(entity);
        }

        testEntities.forEach(entity -> {
            entity.setProduct(productRepository.save(entity.getProduct()));
            repository.save(entity);
            stockRepository.save(new ProductStockEntity(null, entity.getProduct(), 1));
        });

        saveCard(seller, ProductStatus.OFF, ModerationStatus.APPROVED, 1);
        saveCard(seller, ProductStatus.ARCHIVED, ModerationStatus.APPROVED, 1);
        saveCard(seller, ProductStatus.ACTIVE, ModerationStatus.PENDING, 1);
        saveCard(seller, ProductStatus.ACTIVE, ModerationStatus.APPROVED, 0);

        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "product.name"));

        Page<ProductCardEntity> result = repository.findAllByProduct_StatusAndProduct_ModerationStatus(
                ProductStatus.ACTIVE,
                ModerationStatus.APPROVED,
                null,
                null,
                null,
                pageable
        );

        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getTotalElements()).isEqualTo(10);

        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isTrue();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();

        assertThat(result.getContent()).allSatisfy(card -> {
            assertThat(card.getProduct().getStatus()).isEqualTo(ProductStatus.ACTIVE);
            assertThat(card.getProduct().getModerationStatus()).isEqualTo(ModerationStatus.APPROVED);
            assertThat(stockRepository.findById(card.getProduct().getId()).orElseThrow().getQuantity())
                    .isPositive();
        });
    }

    @Test
    void shouldCombineCategoryAndInclusivePriceRangeBeforePagination() {
        UserEntity seller = createSeller();
        ProductEntity lowerBoundary = saveCard(seller, ProductStatus.ACTIVE, ModerationStatus.APPROVED, 1);
        lowerBoundary.setPrice(new BigDecimal("500.00"));
        ProductEntity upperBoundary = saveCard(seller, ProductStatus.ACTIVE, ModerationStatus.APPROVED, 1);
        upperBoundary.setPrice(new BigDecimal("900.00"));
        ProductEntity outsidePrice = saveCard(seller, ProductStatus.ACTIVE, ModerationStatus.APPROVED, 1);
        outsidePrice.setPrice(new BigDecimal("901.00"));
        ProductEntity outsideCategory = saveCard(seller, ProductStatus.ACTIVE, ModerationStatus.APPROVED, 1);
        outsideCategory.setPrice(new BigDecimal("800.00"));
        outsideCategory.setCategory(productCategoryRepository.findBySlug("sport-fitness").orElseThrow());

        Page<ProductCardEntity> result = repository.findAllByProduct_StatusAndProduct_ModerationStatus(
                ProductStatus.ACTIVE,
                ModerationStatus.APPROVED,
                "electronics",
                new BigDecimal("500.00"),
                new BigDecimal("900.00"),
                PageRequest.of(1, 1, Sort.by("product.price"))
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(card -> card.getProduct().getPublicId())
                .containsExactly(upperBoundary.getPublicId());
    }

    private ProductEntity saveCard(
            UserEntity seller,
            ProductStatus status,
            ModerationStatus moderationStatus,
            int quantity
    ) {
        UUID productId = UUID.randomUUID();
        ProductCardEntity card = ProductTestFixtures.createProductCard(productId);
        ProductEntity product = card.getProduct();
        product.setSlug("test-" + productId);
        product.setSeller(seller);
        product.setCategory(productCategoryRepository.findBySlug("electronics").orElseThrow());
        product.setStatus(status);
        product.setModerationStatus(moderationStatus);
        card.setProduct(productRepository.save(product));
        repository.save(card);
        stockRepository.save(new ProductStockEntity(null, card.getProduct(), quantity));
        return card.getProduct();
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
