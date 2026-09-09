package ru.nsu.marketplace.service;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.nsu.marketplace.StaticMethods;
import ru.nsu.marketplace.dto.CreateProductRequest;
import ru.nsu.marketplace.dto.ProductCardResponse;
import ru.nsu.marketplace.dto.ProductDetailsResponse;
import ru.nsu.marketplace.dto.ImagesDetailsResponse;
import ru.nsu.marketplace.dto.UpdateImagesPositionsRequest;
import ru.nsu.marketplace.dto.UpdateProductRequest;

import ru.nsu.marketplace.entity.ProductCardEntity;
import ru.nsu.marketplace.entity.ProductDetailsImageEntity;
import ru.nsu.marketplace.entity.ProductEntity;
import ru.nsu.marketplace.repository.ProductCardsRepository;
import ru.nsu.marketplace.repository.ProductDetailsImageRepository;
import ru.nsu.marketplace.repository.ProductRepository;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.nsu.marketplace.StaticMethods.getTestProductCardEntity;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductCardsRepository repository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductDetailsImageRepository productDetailsImageRepository;

    @Mock
    private MediaStorageService mediaStorageService;

    @InjectMocks
    private ProductService service;

    @Test
    void shouldReturnCards(){
        ProductCardEntity entity = StaticMethods.getTestProductCardEntity(UUID.randomUUID());

        Pageable pageable = PageRequest.of(0,2);

        Page<ProductCardEntity> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(repository.findAll(pageable))
                .thenReturn(page);

        Page<ProductCardResponse> result = service.getCatalog(pageable);

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

        Page<ProductCardResponse> result = service.getCatalog(pageable);

        assertThat(result).isEmpty();
        verify(repository).findAll(pageable);
    }

    @Test
    void shouldReturnByCorrectUUID(){
        ProductEntity entity = StaticMethods.getTestProductCardEntity(UUID.randomUUID()).getProduct();
        UUID uuid = entity.getPublicId();

        when(productRepository.findByPublicId(uuid)).thenReturn(Optional.of(entity));

        ProductDetailsResponse result = service.getProduct(uuid);

        assertThat(result.id()).isEqualTo(uuid);
        verify(productRepository).findByPublicId(uuid);
    }

    @Test
    void shouldReturnByIncorrectUUID(){
        UUID uuid = UUID.randomUUID();

        when(productRepository.findByPublicId(uuid)).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                () -> service.getProduct(uuid));

        assertThat(exception.getMessage()).isEqualTo("Product not found");
        verify(productRepository).findByPublicId(uuid);
    }

    @Test
    void shouldCreateProduct(){
        CreateProductRequest request = new CreateProductRequest(
                "iphone-16-pro",
                "iphone 16 pro",
                "A sufficiently long product description for tests.",
                new BigDecimal("99990.00")
        );

        MultipartFile previewImage = image("previewImage");
        MultipartFile detailsImage = image("detailsImages");

        when(productRepository.save(any(ProductEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(ProductCardEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productDetailsImageRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaStorageService.saveImage(any()))
                .thenReturn("/media/preview.webp", "/media/details.webp");

        ProductDetailsResponse result = service.createProduct(request, previewImage, List.of(detailsImage));

        assertThat(result.id()).isNotNull();
        assertThat(result.slug()).isEqualTo("iphone-16-pro");
        assertThat(result.name()).isEqualTo("iphone 16 pro");
        assertThat(result.price())
                .isEqualByComparingTo("99990.00");
        verify(repository).save(any(ProductCardEntity.class));
    }


    @Test
    void shouldRejectProductWithoutDetailsImages(){
        CreateProductRequest request = new CreateProductRequest(
                "iphone-16-pro",
                "iphone 16 pro",
                "A sufficiently long product description for tests.",
                new BigDecimal("99990.00")
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.createProduct(request, image("previewImage"), List.of()));

        assertThat(exception.getMessage())
                .isEqualTo("At least one details image is required");

        verify(repository, never()).save(any(ProductCardEntity.class));
    }

    @Test
    void shouldCopyRequestDataToEntity() {
        CreateProductRequest request = new CreateProductRequest(
                "iphone-16-pro",
                "iphone 16 pro",
                "A sufficiently long product description for tests.",
                new BigDecimal("99990.00")
        );

        when(productRepository.save(any(ProductEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(ProductCardEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productDetailsImageRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaStorageService.saveImage(any()))
                .thenReturn("/media/preview.webp", "/media/details.webp");

        service.createProduct(request, image("previewImage"), List.of(image("detailsImages")));

        ArgumentCaptor<ProductCardEntity> captor =
                ArgumentCaptor.forClass(ProductCardEntity.class);

        verify(repository).save(captor.capture());

        ProductCardEntity savedEntity = captor.getValue();

        assertThat(savedEntity.getProduct().getPublicId()).isNotNull();
        assertThat(savedEntity.getProduct().getSlug())
                .isEqualTo(request.slug());
        assertThat(savedEntity.getProduct().getName())
                .isEqualTo(request.name());
        assertThat(savedEntity.getProduct().getPrice())
                .isEqualByComparingTo(request.price());
        assertThat(savedEntity.getProduct().getDescription())
                .isEqualTo(request.description());
        assertThat(savedEntity.getImgUrl())
                .isEqualTo("/media/preview.webp");
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
        when(productDetailsImageRepository.save(any(ProductDetailsImageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<ImagesDetailsResponse> response = service.addImages(
                productId, List.of(firstNewImage, secondNewImage)
        );

        assertThat(response).extracting(ImagesDetailsResponse::position)
                .containsExactly(2, 3);
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

        List<ImagesDetailsResponse> response = service.updateImagesPositions(
                productId,
                new UpdateImagesPositionsRequest(List.of(thirdId, firstId, secondId))
        );

        assertThat(response).extracting(ImagesDetailsResponse::uuid)
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

        assertThrows(IllegalArgumentException.class, () -> service.updateImagesPositions(
                productId,
                new UpdateImagesPositionsRequest(List.of(product.getDetailsImages().getFirst().getPublicId()))
        ));
    }

    @Test
    void shouldRejectDuplicateImageIdsInOrder() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = productWithImages(productId, 2);
        UUID imageId = product.getDetailsImages().getFirst().getPublicId();

        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class, () -> service.updateImagesPositions(
                productId,
                new UpdateImagesPositionsRequest(List.of(imageId, imageId))
        ));
    }

    @Test
    void shouldRejectImageOfAnotherProductInOrder() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = productWithImages(productId, 2);

        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class, () -> service.updateImagesPositions(
                productId,
                new UpdateImagesPositionsRequest(List.of(
                        product.getDetailsImages().getFirst().getPublicId(),
                        UUID.randomUUID()
                ))
        ));
    }

    @Test
    void shouldDeleteDetailsImageAndRenumberRemainingImages() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = productWithImages(productId, 3);
        ProductDetailsImageEntity deletedImage = product.getDetailsImages().get(1);

        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));

        ImagesDetailsResponse response = service.deleteProductDetailsImage(
                deletedImage.getPublicId(), productId
        );

        assertThat(response.uuid()).isEqualTo(deletedImage.getPublicId());
        assertThat(product.getDetailsImages()).hasSize(2);
        assertThat(product.getDetailsImages()).extracting(ProductDetailsImageEntity::getPosition)
                .containsExactly(0, 1);
        verify(productDetailsImageRepository).delete(deletedImage);
        verify(mediaStorageService).deleteImage(deletedImage.getImageUrl());
    }

    @Test
    void shouldNotDeleteDetailsImageOfAnotherProduct() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = productWithImages(productId, 1);

        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class, () -> service.deleteProductDetailsImage(
                UUID.randomUUID(), productId
        ));

        verifyNoInteractions(productDetailsImageRepository, mediaStorageService);
    }

    @Test
    void shouldReplacePreviewImage() {
        UUID productId = UUID.randomUUID();
        ProductCardEntity card = StaticMethods.getTestProductCardEntity(productId);
        MultipartFile previewImage = image("previewImage");

        when(repository.findByProduct_PublicId(productId)).thenReturn(Optional.of(card));
        when(mediaStorageService.saveImage(previewImage)).thenReturn("/media/new-preview.webp");

        ProductCardResponse response = service.updatePreview(productId, previewImage);

        assertThat(card.getImgUrl()).isEqualTo("/media/new-preview.webp");
        assertThat(response.imgUrl()).isEqualTo("/media/new-preview.webp");
        verify(mediaStorageService).deleteImage("/media/products/iphone.webp");
    }

    @Test
    void shouldDeleteProductWithItsCardAndImages() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = productWithImages(productId, 2);
        ProductCardEntity card = new ProductCardEntity();
        card.setProduct(product);
        card.setImgUrl("/media/preview.webp");

        when(repository.findByProduct_PublicId(productId)).thenReturn(Optional.of(card));
        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));

        service.deleteProduct(productId);

        verify(repository).delete(card);
        verify(productDetailsImageRepository).delete(product.getDetailsImages().get(0));
        verify(productDetailsImageRepository).delete(product.getDetailsImages().get(1));
        verify(productRepository).delete(product);
        verify(mediaStorageService).deleteImage("/media/preview.webp");
        verify(mediaStorageService).deleteImage("/media/image-0.webp");
        verify(mediaStorageService).deleteImage("/media/image-1.webp");
    }

    @Test
    void shouldUpdateOnlyProvidedProductFields() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = StaticMethods.getTestProductCardEntity(productId).getProduct();

        when(productRepository.findByPublicId(productId)).thenReturn(Optional.of(product));

        service.updateProduct(productId, new UpdateProductRequest(
                null, "New name", null, new BigDecimal("109990.00")
        ));

        assertThat(product.getName()).isEqualTo("New name");
        assertThat(product.getPrice()).isEqualByComparingTo("109990.00");
        assertThat(product.getSlug()).isEqualTo("iphone-16-pro");
        assertThat(product.getDescription()).isEqualTo("A sufficiently long product description for tests.");
    }

    private ProductEntity productWithImages(UUID productId, int imageCount) {
        ProductEntity product = StaticMethods.getTestProductCardEntity(productId).getProduct();

        for (int position = 0; position < imageCount; position++) {
            ProductDetailsImageEntity image = new ProductDetailsImageEntity();
            image.setPublicId(UUID.randomUUID());
            image.setProduct(product);
            image.setImageUrl("/media/image-" + position + ".webp");
            image.setPosition(position);
            product.getDetailsImages().add(image);
        }

        return product;
    }

    private MockMultipartFile image(String partName) {
        return new MockMultipartFile(partName, "image.webp", "image/webp", "image".getBytes());
    }
}
