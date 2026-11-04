package ru.nsu.marketplace.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import ru.nsu.marketplace.media.MediaStorageService;
import ru.nsu.marketplace.product.dto.CreateProductRequest;
import ru.nsu.marketplace.product.dto.ProductCardResponse;
import ru.nsu.marketplace.product.dto.ProductDetailsImageResponse;
import ru.nsu.marketplace.product.dto.ProductDetailsResponse;
import ru.nsu.marketplace.product.dto.UpdateProductImageOrderRequest;
import ru.nsu.marketplace.product.dto.UpdateProductRequest;
import ru.nsu.marketplace.product.exception.ProductNotFoundException;
import ru.nsu.marketplace.product.model.ProductCardEntity;
import ru.nsu.marketplace.product.model.ProductDetailsImageEntity;
import ru.nsu.marketplace.product.model.ProductEntity;
import ru.nsu.marketplace.user.Role;
import ru.nsu.marketplace.user.UserEntity;
import ru.nsu.marketplace.user.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductCardRepository productCardRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductDetailsImageRepository productDetailsImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MediaStorageService mediaStorageService;

    @InjectMocks
    private ProductService service;

    @Test
    void shouldReturnCards() {
        ProductCardEntity entity = ProductTestFixtures.createProductCard(UUID.randomUUID());

        Pageable pageable = PageRequest.of(0, 2);

        Page<ProductCardEntity> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(productCardRepository.findAll(pageable)).thenReturn(page);

        Page<ProductCardResponse> result = service.getCatalogProductCards(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().name()).isEqualTo("iphone 16 pro");
        assertThat(result.getContent().getFirst().price()).isEqualByComparingTo("99990.00");

        verify(productCardRepository).findAll(pageable);
    }

    @Test
    void shouldReturnEmptyPage() {
        Page<ProductCardEntity> emptyPage = new PageImpl<>(List.of());

        Pageable pageable = PageRequest.of(0, 2);

        when(productCardRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<ProductCardResponse> result = service.getCatalogProductCards(pageable);

        assertThat(result).isEmpty();
        verify(productCardRepository).findAll(pageable);
    }

    @Test
    void shouldReturnByCorrectUUID() {
        ProductEntity entity = ProductTestFixtures.createProductCard(UUID.randomUUID()).getProduct();
        UUID uuid = entity.getPublicId();

        when(productRepository.findByPublicId(uuid)).thenReturn(Optional.of(entity));

        ProductDetailsResponse result = service.getProduct(uuid);

        assertThat(result.id()).isEqualTo(uuid);
        verify(productRepository).findByPublicId(uuid);
    }

    @Test
    void shouldReturnByIncorrectUUID() {
        UUID uuid = UUID.randomUUID();

        when(productRepository.findByPublicId(uuid)).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class, () ->
            service.getProduct(uuid)
        );

        assertThat(exception.getMessage()).isEqualTo("Product not found");
        verify(productRepository).findByPublicId(uuid);
    }

    @Test
    void shouldCreateProduct() {
        CreateProductRequest request = new CreateProductRequest(
            "iphone-16-pro",
            "iphone 16 pro",
            "A sufficiently long product description for tests.",
            new BigDecimal("99990.00")
        );

        MultipartFile previewImage = image("previewImage");
        MultipartFile detailsImage = image("detailsImages");

        when(userRepository.findById(10L)).thenReturn(Optional.of(seller()));

        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );
        when(productCardRepository.save(any(ProductCardEntity.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );
        when(productDetailsImageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaStorageService.saveImage(any())).thenReturn("/media/preview.webp", "/media/details.webp");

        ProductDetailsResponse result = service.createProduct(
            request,
            previewImage,
            List.of(detailsImage),
            10L
        );

        assertThat(result.id()).isNotNull();
        assertThat(result.slug()).isEqualTo("iphone-16-pro");
        assertThat(result.name()).isEqualTo("iphone 16 pro");
        assertThat(result.price()).isEqualByComparingTo("99990.00");
        verify(productCardRepository).save(any(ProductCardEntity.class));
    }

    @Test
    void shouldRejectProductWithoutDetailsImages() {
        CreateProductRequest request = new CreateProductRequest(
            "iphone-16-pro",
            "iphone 16 pro",
            "A sufficiently long product description for tests.",
            new BigDecimal("99990.00")
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            service.createProduct(request, image("previewImage"), List.of(), 10L)
        );

        assertThat(exception.getMessage()).isEqualTo("At least one details image is required");

        verify(productCardRepository, never()).save(any(ProductCardEntity.class));
    }

    @Test
    void shouldCopyRequestDataToEntity() {
        CreateProductRequest request = new CreateProductRequest(
            "iphone-16-pro",
            "iphone 16 pro",
            "A sufficiently long product description for tests.",
            new BigDecimal("99990.00")
        );

        when(userRepository.findById(10L)).thenReturn(Optional.of(seller()));

        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );
        when(productCardRepository.save(any(ProductCardEntity.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );
        when(productDetailsImageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaStorageService.saveImage(any())).thenReturn("/media/preview.webp", "/media/details.webp");

        service.createProduct(request, image("previewImage"), List.of(image("detailsImages")), 10L);

        ArgumentCaptor<ProductCardEntity> captor = ArgumentCaptor.forClass(ProductCardEntity.class);

        verify(productCardRepository).save(captor.capture());

        ProductCardEntity savedEntity = captor.getValue();

        assertThat(savedEntity.getProduct().getSeller().getId()).isEqualTo(10L);
        assertThat(savedEntity.getProduct().getPublicId()).isNotNull();
        assertThat(savedEntity.getProduct().getSlug()).isEqualTo(request.slug());
        assertThat(savedEntity.getProduct().getName()).isEqualTo(request.name());
        assertThat(savedEntity.getProduct().getPrice()).isEqualByComparingTo(request.price());
        assertThat(savedEntity.getProduct().getDescription()).isEqualTo(request.description());
        assertThat(savedEntity.getPreviewImageUrl()).isEqualTo("/media/preview.webp");
    }

    @Test
    void shouldAddImagesAfterExistingImages() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = productWithImages(productId, 2);
        MultipartFile firstNewImage = image("images");
        MultipartFile secondNewImage = image("images");

        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));
        when(mediaStorageService.saveImage(firstNewImage)).thenReturn("/media/third.webp");
        when(mediaStorageService.saveImage(secondNewImage)).thenReturn("/media/fourth.webp");
        when(productDetailsImageRepository.save(any(ProductDetailsImageEntity.class))).thenAnswer(
            invocation -> invocation.getArgument(0)
        );

        List<ProductDetailsImageResponse> response = service.addProductDetailsImages(
            productId,
            List.of(firstNewImage, secondNewImage),
            10L
        );

        assertThat(response).extracting(ProductDetailsImageResponse::position).containsExactly(2, 3);
        assertThat(product.getDetailsImages()).hasSize(4);
    }

    @Test
    void shouldUpdateImagesPositions() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = productWithImages(productId, 3);
        UUID firstId = product.getDetailsImages().get(0).getPublicId();
        UUID secondId = product.getDetailsImages().get(1).getPublicId();
        UUID thirdId = product.getDetailsImages().get(2).getPublicId();

        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));

        List<ProductDetailsImageResponse> response = service.updateProductImageOrder(
            productId,
            new UpdateProductImageOrderRequest(List.of(thirdId, firstId, secondId)),
            10L
        );

        assertThat(response)
            .extracting(ProductDetailsImageResponse::uuid)
            .containsExactly(thirdId, firstId, secondId);
        assertThat(product.getDetailsImages().get(0).getPosition()).isEqualTo(1);
        assertThat(product.getDetailsImages().get(1).getPosition()).isEqualTo(2);
        assertThat(product.getDetailsImages().get(2).getPosition()).isEqualTo(0);
    }

    @Test
    void shouldRejectIncompleteImageOrder() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = productWithImages(productId, 2);

        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class, () ->
            service.updateProductImageOrder(
                productId,
                new UpdateProductImageOrderRequest(
                    List.of(product.getDetailsImages().getFirst().getPublicId())
                ),
                10L
            )
        );
    }

    @Test
    void shouldRejectDuplicateImageIdsInOrder() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = productWithImages(productId, 2);
        UUID imageId = product.getDetailsImages().getFirst().getPublicId();

        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class, () ->
            service.updateProductImageOrder(
                productId,
                new UpdateProductImageOrderRequest(List.of(imageId, imageId)),
                10L
            )
        );
    }

    @Test
    void shouldRejectImageOfAnotherProductInOrder() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = productWithImages(productId, 2);

        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class, () ->
            service.updateProductImageOrder(
                productId,
                new UpdateProductImageOrderRequest(
                    List.of(product.getDetailsImages().getFirst().getPublicId(), UUID.randomUUID())
                ),
                10L
            )
        );
    }

    @Test
    void shouldDeleteDetailsImageAndRenumberRemainingImages() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = productWithImages(productId, 3);
        ProductDetailsImageEntity deletedImage = product.getDetailsImages().get(1);

        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));

        ProductDetailsImageResponse response = service.deleteProductDetailsImage(
            deletedImage.getPublicId(),
            productId,
            10L
        );

        assertThat(response.uuid()).isEqualTo(deletedImage.getPublicId());
        assertThat(product.getDetailsImages()).hasSize(2);
        assertThat(product.getDetailsImages())
            .extracting(ProductDetailsImageEntity::getPosition)
            .containsExactly(0, 1);
        verify(productDetailsImageRepository).delete(deletedImage);
        verify(mediaStorageService).deleteImage(deletedImage.getImageUrl());
    }

    @Test
    void shouldNotDeleteDetailsImageOfAnotherProduct() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = productWithImages(productId, 1);

        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class, () ->
            service.deleteProductDetailsImage(UUID.randomUUID(), productId, 10L)
        );

        verifyNoInteractions(productDetailsImageRepository, mediaStorageService);
    }

    @Test
    void shouldReplacePreviewImage() {
        UUID productId = UUID.randomUUID();
        ProductCardEntity card = ProductTestFixtures.createProductCard(productId);
        MultipartFile previewImage = image("previewImage");

        when(productCardRepository.findByProduct_PublicId(productId)).thenReturn(Optional.of(card));
        when(mediaStorageService.saveImage(previewImage)).thenReturn("/media/new-preview.webp");

        ProductCardResponse response = service.updateProductPreview(productId, previewImage, 10L);

        assertThat(card.getPreviewImageUrl()).isEqualTo("/media/new-preview.webp");
        assertThat(response.imgUrl()).isEqualTo("/media/new-preview.webp");
        verify(mediaStorageService).deleteImage("/media/products/iphone.webp");
    }

    @Test
    void shouldArchiveProductWithoutDeletingItsCardAndImages() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = productWithImages(productId, 2);
        ProductCardEntity card = new ProductCardEntity();
        card.setProduct(product);
        card.setPreviewImageUrl("/media/preview.webp");

        product.setStatus(ru.nsu.marketplace.product.model.ProductStatus.OFF);
        product.setModerationStatus(ru.nsu.marketplace.common.moderation.ModerationStatus.NOT_SUBMITTED);
        when(productRepository.findByPublicIdForUpdate(productId)).thenReturn(Optional.of(product));

        service.archiveProduct(productId, 10L);

        assertThat(product.getStatus()).isEqualTo(ru.nsu.marketplace.product.model.ProductStatus.ARCHIVED);
        assertThat(product.getDetailsImages()).hasSize(2);
        verify(productRepository, never()).delete(any());
        verifyNoInteractions(productCardRepository, productDetailsImageRepository, mediaStorageService);
    }

    @Test
    void shouldUpdateOnlyProvidedProductFields() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = ProductTestFixtures.createProductCard(productId).getProduct();

        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));

        service.updateProduct(
            productId,
            10L,
            new UpdateProductRequest(null, "New name", null, new BigDecimal("109990.00"))
        );

        assertThat(product.getName()).isEqualTo("New name");
        assertThat(product.getPrice()).isEqualByComparingTo("109990.00");
        assertThat(product.getSlug()).isEqualTo("iphone-16-pro");
        assertThat(product.getDescription()).isEqualTo("A sufficiently long product description for tests.");
    }

    @Test
    void shouldRejectProductCreationByBuyer() {
        CreateProductRequest request = new CreateProductRequest(
            "iphone-16-pro",
            "iphone 16 pro",
            "A sufficiently long product description for tests.",
            new BigDecimal("99990.00")
        );
        UserEntity buyer = UserEntity.builder().id(10L).roles(Set.of(Role.BUYER)).build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(buyer));

        assertThrows(AccessDeniedException.class, () ->
            service.createProduct(request, image("previewImage"), List.of(image("detailsImages")), 10L)
        );

        verifyNoInteractions(
            productRepository,
            productCardRepository,
            productDetailsImageRepository,
            mediaStorageService
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = { "fields", "addProductDetailsImages", "positions", "deleteImage", "preview", "archive" }
    )
    void shouldRejectModificationByAnotherSeller(String operation) {
        assertModificationDenied(operation, 20L, Set.of(Role.SELLER));
    }

    @ParameterizedTest
    @ValueSource(
        strings = { "fields", "addProductDetailsImages", "positions", "deleteImage", "preview", "archive" }
    )
    void shouldRejectModificationWhenOwnerIsNoLongerSeller(String operation) {
        assertModificationDenied(operation, 10L, Set.of(Role.BUYER));
    }

    private void assertModificationDenied(String operation, Long userId, Set<Role> ownerRoles) {
        UUID productId = UUID.randomUUID();
        ProductEntity product = productWithImages(productId, 2);
        product.getSeller().setRoles(ownerRoles);
        ProductCardEntity card = new ProductCardEntity();
        card.setProduct(product);
        card.setPreviewImageUrl("/media/preview.webp");

        if (operation.equals("preview") || operation.equals("archive")) {
            when(productCardRepository.findByProduct_PublicId(productId)).thenReturn(Optional.of(card));
        }
        if (!operation.equals("preview")) {
            when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));
        }

        assertThrows(AccessDeniedException.class, () -> {
            switch (operation) {
                case "fields" -> service.updateProduct(
                    productId,
                    userId,
                    new UpdateProductRequest(null, "New name", null, null)
                );
                case "addProductDetailsImages" -> service.addProductDetailsImages(
                    productId,
                    List.of(image("images")),
                    userId
                );
                case "positions" -> service.updateProductImageOrder(
                    productId,
                    new UpdateProductImageOrderRequest(
                        List.of(
                            product.getDetailsImages().get(1).getPublicId(),
                            product.getDetailsImages().get(0).getPublicId()
                        )
                    ),
                    userId
                );
                case "deleteImage" -> service.deleteProductDetailsImage(
                    product.getDetailsImages().getFirst().getPublicId(),
                    productId,
                    userId
                );
                case "preview" -> service.updateProductPreview(productId, image("previewImage"), userId);
                case "archive" -> service.archiveProduct(productId, userId);
                default -> throw new IllegalArgumentException("Unknown operation: " + operation);
            }
        });

        assertThat(product.getName()).isEqualTo("iphone 16 pro");
        assertThat(product.getDetailsImages()).hasSize(2);
        assertThat(product.getDetailsImages())
            .extracting(ProductDetailsImageEntity::getPosition)
            .containsExactly(0, 1);
        assertThat(card.getPreviewImageUrl()).isEqualTo("/media/preview.webp");
        verifyNoInteractions(productDetailsImageRepository, mediaStorageService);
        verify(productRepository, never()).save(any());
        verify(productRepository, never()).delete(any());
        verify(productCardRepository, never()).save(any());
        verify(productCardRepository, never()).delete(any());
    }

    private UserEntity seller() {
        return UserEntity.builder().id(10L).roles(Set.of(Role.SELLER)).build();
    }

    private ProductEntity productWithImages(UUID productId, int imageCount) {
        ProductEntity product = ProductTestFixtures.createProductCard(productId).getProduct();

        for (int position = 0; position < imageCount; position++) {
            ProductDetailsImageEntity image = ProductDetailsImageEntity.builder()
                .publicId(UUID.randomUUID())
                .product(product)
                .imageUrl("/media/image-" + position + ".webp")
                .position(position)
                .build();
            product.getDetailsImages().add(image);
        }

        return product;
    }

    private MockMultipartFile image(String partName) {
        return new MockMultipartFile(partName, "image.webp", "image/webp", "image".getBytes());
    }
}
