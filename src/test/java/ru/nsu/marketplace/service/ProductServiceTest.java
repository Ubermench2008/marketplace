package ru.nsu.marketplace.service;

import exceptions.ImageNotFoundException;
import exceptions.ProductNotFoundException;
import org.springframework.data.domain.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.nsu.marketplace.dto.CreateProductRequest;
import ru.nsu.marketplace.dto.ProductCardResponse;

import ru.nsu.marketplace.entity.ProductCardEntity;
import ru.nsu.marketplace.entity.ProductEntity;
import ru.nsu.marketplace.repository.ProductCardsRepository;
import ru.nsu.marketplace.repository.ProductRepository;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ru.nsu.marketplace.StaticMethods.getTestEntity;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductCardsRepository repository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService service;

    @Test
    void shouldReturnCards(){
        ProductCardEntity entity = getTestEntity(UUID.randomUUID());

        Pageable pageable = PageRequest.of(0,2);

        Page<ProductCardEntity> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(repository.findAll(pageable))
                .thenReturn(page);

        Page<ProductCardResponse> result = service.getAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().name())
                .isEqualTo("iphone 16 pro");
        assertThat(result.getContent().getFirst().price())
                .isEqualByComparingTo("99990.00");

        verify(repository).findAll(pageable);
    }

    @Test
    void shouldReturnEmptyPage(){
        Page<ProductCardEntity> emptyPage = new PageImpl<>(List.of());

        Pageable pageable = PageRequest.of(0, 2);

        when(repository.findAll(pageable))
                .thenReturn(emptyPage);

        Page<ProductCardResponse> result = service.getAll(pageable);

        assertThat(result).isEmpty();
        verify(repository).findAll(pageable);
    }

    @Test
    void shouldReturnByCorrectUUID(){
        Optional<ProductCardEntity> entity = Optional.of(getTestEntity(UUID.randomUUID()));
        UUID uuid = entity.orElseThrow().getPublicId();

        when(repository.findByPublicId(uuid))
                .thenReturn(entity);

        ProductCardResponse result = service.getById(uuid);

        assertThat(result.id()).isEqualTo(uuid);
        verify(repository).findByPublicId(uuid);
    }

    @Test
    void shouldReturnByIncorrectUUID(){
        Optional<ProductCardEntity> emptyEntity = Optional.empty();

        UUID uuid = UUID.randomUUID();

        when(repository.findByPublicId(uuid))
                .thenReturn(emptyEntity);

        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                () -> service.getById(uuid));

        assertThat(exception.getMessage()).isEqualTo("Product not found");
        verify(repository).findByPublicId(uuid);
    }

    @Test
    void shouldCreateProduct(){
        CreateProductRequest request = new CreateProductRequest(
                "iphone-16-pro",
                "iphone 16 pro",
                "A sufficiently long product description for tests.",
                new BigDecimal("99990.00"),
                "/media/products/iphone.webp"
        );

        ProductCardEntity saved = getTestEntity(UUID.randomUUID());

        when(productRepository.save(any(ProductEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(ProductCardEntity.class)))
                .thenReturn(saved);

        ProductCardResponse result = service.createProduct(request);

        assertThat(result.id()).isNotNull();
        assertThat(result.slug()).isEqualTo("iphone-16-pro");
        assertThat(result.name()).isEqualTo("iphone 16 pro");
        assertThat(result.price())
                .isEqualByComparingTo("99990.00");
        assertThat(result.imgUrl())
                .isEqualTo("/media/products/iphone.webp");

        verify(repository).save(any(ProductCardEntity.class));
    }


    @Test
    void shouldCreateProductWithIncorrectImgUrl(){
        CreateProductRequest request = new CreateProductRequest(
                "iphone-16-pro",
                "iphone 16 pro",
                "A sufficiently long product description for tests.",
                new BigDecimal("99990.00"),
                "/media/products/incorrect.webp"
        );

        ImageNotFoundException exception = assertThrows(ImageNotFoundException.class,
                () -> service.createProduct(request));

        assertThat(exception.getMessage())
                .isEqualTo("Image not found: " + request.imgUrl());

        verify(repository, never()).save(any(ProductCardEntity.class));
    }

    @Test
    void shouldCopyRequestDataToEntity() {
        CreateProductRequest request = new CreateProductRequest(
                "iphone-16-pro",
                "iphone 16 pro",
                "A sufficiently long product description for tests.",
                new BigDecimal("99990.00"),
                "/media/products/iphone.webp"
        );

        when(productRepository.save(any(ProductEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(ProductCardEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createProduct(request);

        ArgumentCaptor<ProductCardEntity> captor =
                ArgumentCaptor.forClass(ProductCardEntity.class);

        verify(repository).save(captor.capture());

        ProductCardEntity savedEntity = captor.getValue();

        assertThat(savedEntity.getPublicId()).isNotNull();
        assertThat(savedEntity.getProduct().getSlug())
                .isEqualTo(request.slug());
        assertThat(savedEntity.getProduct().getName())
                .isEqualTo(request.name());
        assertThat(savedEntity.getProduct().getPrice())
                .isEqualByComparingTo(request.price());
        assertThat(savedEntity.getProduct().getDescription())
                .isEqualTo(request.description());
        assertThat(savedEntity.getImgUrl())
                .isEqualTo(request.imgUrl());
    }
}
