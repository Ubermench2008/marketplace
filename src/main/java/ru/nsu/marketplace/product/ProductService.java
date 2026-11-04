package ru.nsu.marketplace.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.nsu.marketplace.common.moderation.ModerationStatus;
import ru.nsu.marketplace.media.MediaImageDeletionRequested;
import ru.nsu.marketplace.media.MediaStorageService;
import ru.nsu.marketplace.product.dto.CreateProductRequest;
import ru.nsu.marketplace.product.dto.ProductCardResponse;
import ru.nsu.marketplace.product.dto.ProductDetailsImageResponse;
import ru.nsu.marketplace.product.dto.ProductDetailsResponse;
import ru.nsu.marketplace.product.dto.UpdateProductImageOrderRequest;
import ru.nsu.marketplace.product.dto.UpdateProductRequest;
import ru.nsu.marketplace.product.exception.ProductModerationStateException;
import ru.nsu.marketplace.product.exception.ProductNotFoundException;
import ru.nsu.marketplace.product.exception.ProductStateException;
import ru.nsu.marketplace.product.model.ProductCardEntity;
import ru.nsu.marketplace.product.model.ProductDetailsImageEntity;
import ru.nsu.marketplace.product.model.ProductEntity;
import ru.nsu.marketplace.product.model.ProductStatus;
import ru.nsu.marketplace.product.moderation.ProductModerationEntity;
import ru.nsu.marketplace.product.moderation.ProductModerationRepository;
import ru.nsu.marketplace.product.moderation.ProductModerationSnapshot;
import ru.nsu.marketplace.user.Role;
import ru.nsu.marketplace.user.UserEntity;
import ru.nsu.marketplace.user.UserRepository;
import ru.nsu.marketplace.user.exception.UserNotFoundException;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductCardRepository productCardRepository;
    private final ProductRepository productRepository;
    private final ProductModerationRepository productModerationRepository;
    private final ProductDetailsImageRepository productDetailsImageRepository;
    private final UserRepository userRepository;
    private final MediaStorageService mediaStorageService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<ProductCardResponse> getCatalogProductCards(Pageable pageable) {
        return productCardRepository.findAll(pageable)
                .map(this::toCardResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductCardResponse> getSellerProductCards(Pageable pageable, Long sellerId) {
        return productCardRepository.findAllByProduct_Seller_Id(sellerId, pageable)
                .map(this::toCardResponse);
    }

    @Transactional(readOnly = true)
    public ProductDetailsResponse getProduct(UUID productId) {
        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        return toDetailsResponse(product);
    }

    @Transactional
    public ProductDetailsResponse createProduct(
            CreateProductRequest request,
            MultipartFile previewImage,
            List<MultipartFile> detailsImages,
            Long userId
    ) {
        if (detailsImages == null || detailsImages.isEmpty()) {
            throw new IllegalArgumentException("At least one details image is required");
        }

        UserEntity seller = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("Seller not found: userId=" + userId)
        );

        ProductEntity product = ProductEntity.builder()
                .publicId(UUID.randomUUID())
                .slug(request.slug())
                .name(request.name())
                .price(request.price())
                .description(request.description())
                .seller(seller)
                .build();

        ensureSellerOwnsProduct(product, userId);

        ProductEntity savedProduct = productRepository.save(product);

        ProductCardEntity card = new ProductCardEntity();

        card.setPreviewImageUrl(mediaStorageService.saveImage(previewImage));
        card.setProduct(savedProduct);

        productCardRepository.save(card);

        int position = 0;

        for (MultipartFile detailsImage : detailsImages) {
            ProductDetailsImageEntity imageEntity = ProductDetailsImageEntity.builder()
                    .publicId(UUID.randomUUID())
                    .product(savedProduct)
                    .imageUrl(mediaStorageService.saveImage(detailsImage))
                    .position(position++)
                    .build();

            productDetailsImageRepository.save(imageEntity);
            savedProduct.getDetailsImages().add(imageEntity);
        }
        log.info("Product was created: productId={}", savedProduct.getPublicId());

        return toDetailsResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public List<ProductDetailsImageResponse> getProductDetailsImages(UUID productId) {
        ProductEntity product = productRepository.findByPublicId(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        return product.getDetailsImages().stream()
                .map(this::toDetailsImageResponse)
                .toList();

    }

    @Transactional
    public List<ProductDetailsImageResponse> addProductDetailsImages(
            UUID productId,
            List<MultipartFile> images,
            Long userId
    ) {
        ProductEntity product = productRepository.findByPublicIdForUpdate(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerCanEditProduct(product, userId);

        List<ProductDetailsImageResponse> response = new ArrayList<>();

        for (MultipartFile imageFile : images) {
            UUID imagePublicId = UUID.randomUUID();
            String imageUrl = mediaStorageService.saveImage(imageFile);
            int position = product.getDetailsImages().size();

            ProductDetailsImageEntity image = ProductDetailsImageEntity.builder()
                    .publicId(imagePublicId)
                    .product(product)
                    .imageUrl(imageUrl)
                    .position(position)
                    .build();

            productDetailsImageRepository.save(image);
            product.getDetailsImages().add(image);
            response.add(toDetailsImageResponse(image));
        }

        if (!response.isEmpty()) {
            product.setModerationStatus(ModerationStatus.NOT_SUBMITTED);
        }

        List<UUID> addedImageIds = response.stream()
                .map(ProductDetailsImageResponse::uuid)
                .toList();

        log.info("Product details images added: productId={}, imageIds={}",
                productId, addedImageIds);

        return response;
    }

    @Transactional
    public ProductDetailsResponse updateProduct(UUID productId, Long userId, UpdateProductRequest request) {
        ProductEntity product = productRepository.findByPublicIdForUpdate(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerCanEditProduct(product, userId);

        boolean changed = false;

        if (request.slug() != null && !Objects.equals(request.slug(), product.getSlug())) {
            product.setSlug(request.slug());
            changed = true;
        }

        if (request.name() != null && !Objects.equals(request.name(), product.getName())) {
            product.setName(request.name());
            changed = true;
        }

        if (request.description() != null && !Objects.equals(request.description(), product.getDescription())) {
            product.setDescription(request.description());
            changed = true;
        }

        if (request.price() != null && request.price().compareTo(product.getPrice()) != 0) {
            product.setPrice(request.price());
            changed = true;
        }

        if (changed) {
            product.setModerationStatus(ModerationStatus.NOT_SUBMITTED);
            log.info("Product was updated: productId={}", productId);
        }

        return toDetailsResponse(product);
    }

    @Transactional
    public List<ProductDetailsImageResponse> updateProductImageOrder(
            UUID productId,
            UpdateProductImageOrderRequest request,
            Long userId
    ) {
        ProductEntity product = productRepository.findByPublicIdForUpdate(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerCanEditProduct(product, userId);

        List<ProductDetailsImageEntity> images = product.getDetailsImages();
        List<UUID> imageIds = request.imageIds();

        if (imageIds.size() != images.size()) {
            throw new IllegalArgumentException("All product image IDs must be provided");
        }

        Set<UUID> requestedIds = new HashSet<>(imageIds);
        if (requestedIds.size() != imageIds.size()) {
            throw new IllegalArgumentException("Image IDs must not be duplicated");
        }

        Map<UUID, ProductDetailsImageEntity> imagesById = new HashMap<>();
        for (ProductDetailsImageEntity image : images) {
            imagesById.put(image.getPublicId(), image);
        }

        if (!imagesById.keySet().equals(requestedIds)) {
            throw new IllegalArgumentException("Image IDs do not belong to this product");
        }

        List<UUID> previousOrder = images.stream()
                .sorted(Comparator.comparingInt(ProductDetailsImageEntity::getPosition))
                .map(ProductDetailsImageEntity::getPublicId)
                .toList();

        if (previousOrder.equals(imageIds)) {
            return imageIds.stream()
                    .map(imagesById::get)
                    .map(this::toDetailsImageResponse)
                    .toList();
        }

        List<ProductDetailsImageResponse> response = new ArrayList<>();
        for (int position = 0; position < imageIds.size(); position++) {
            ProductDetailsImageEntity image = imagesById.get(imageIds.get(position));
            image.setPosition(position);

            response.add(toDetailsImageResponse(image));
        }

        product.setModerationStatus(ModerationStatus.NOT_SUBMITTED);

        log.info("Product details images positions updated: \n" +
                        "productId={} \n" +
                        "previousOrder={} \n" +
                        "currentOrder={}",
                productId,
                previousOrder,
                imageIds);

        return response;
    }

    @Transactional
    public ProductDetailsImageResponse deleteProductDetailsImage(UUID imageId, UUID productId, Long userId) {
        ProductEntity product = productRepository.findByPublicIdForUpdate(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerCanEditProduct(product, userId);

        ProductDetailsImageEntity image = product.getDetailsImages().stream()
                .filter(item -> item.getPublicId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Image does not belong to this product"
                ));

        ProductDetailsImageResponse response = toDetailsImageResponse(image);

        product.getDetailsImages().remove(image);
        renumberImagePositions(product.getDetailsImages());

        productDetailsImageRepository.delete(image);
        product.setModerationStatus(ModerationStatus.NOT_SUBMITTED);
        deleteImageUnlessReferencedBySnapshot(image.getImageUrl());

        log.info("Product details image was deleted: \n" +
                        "productId={} \n" +
                        "imageId={}",
                productId,
                imageId);

        return response;
    }

    @Transactional
    public ProductCardResponse updateProductPreview(UUID productId, MultipartFile preview, Long userId) {
        ProductEntity product = productRepository.findByPublicIdForUpdate(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerCanEditProduct(product, userId);

        ProductCardEntity card = productCardRepository
                .findByProduct_PublicId(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        String oldImageUrl = card.getPreviewImageUrl();
        if (mediaStorageService.hasSameContent(oldImageUrl, preview)) {
            return toCardResponse(card);
        }

        String newImageUrl = mediaStorageService.saveImage(preview);

        card.setPreviewImageUrl(newImageUrl);
        product.setModerationStatus(ModerationStatus.NOT_SUBMITTED);
        deleteImageUnlessReferencedBySnapshot(oldImageUrl);

        log.info("Product preview was updated: productId={}", productId);

        return toCardResponse(card);
    }

//    @Transactional
//    public void deleteProduct(UUID productId, Long userId) {
//        ProductCardEntity card = productCardsRepository.findByProduct_PublicId(productId).orElseThrow(
//                () -> new ProductNotFoundException("Product not found")
//        );
//
//        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
//                () -> new ProductNotFoundException("Product not found")
//        );
//
//        ensureSellerOwnsProduct(product, userId);
//
//        String previewUrl = card.getPreviewImageUrl();
//
//        mediaStorageService.deleteImage(previewUrl);
//
//        productCardsRepository.delete(card);
//
//        deleteAllProductDetailsImages(product);
//
//        productRepository.delete(product);
//
//        log.info("Product was deleted: productId={}", productId);
//    }

    @Transactional
    public void submitProductForModeration(UUID productId, Long sellerId) {
        ProductEntity product = productRepository.findByPublicIdForUpdate(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerCanSubmitProductForModeration(product, sellerId);

        ProductCardEntity card = productCardRepository.findByProduct_PublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product card not found")
        );
        List<String> imageUrls = product.getDetailsImages().stream()
                .sorted(Comparator.comparingInt(ProductDetailsImageEntity::getPosition))
                .map(ProductDetailsImageEntity::getImageUrl)
                .toList();

        if (imageUrls.isEmpty()) {
            throw new ProductStateException("At least one details image is required for moderation");
        }

        ProductModerationSnapshot snapshot = ProductModerationSnapshot.builder()
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .previewImageUrl(card.getPreviewImageUrl())
                .build();

        productModerationRepository.save(ProductModerationEntity.builder()
                        .product(product)
                        .publicId(UUID.randomUUID())
                        .snapshot(snapshot)
                        .snapshotImageUrls(new ArrayList<>(imageUrls))
                        .build());

        product.setModerationStatus(ModerationStatus.PENDING);

    }

    @Transactional
    public void cancelProductModerationRequest(UUID productId, Long sellerId) {
        ProductModerationEntity request = productModerationRepository.findPendingByProductPublicIdForUpdate(productId)
                .orElseThrow(() -> new ProductModerationStateException("Product has no pending moderation request"));

        ProductEntity product = productRepository.findByIdForUpdate(request.getProduct().getId()).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerOwnsProduct(product, sellerId);

        request.setStatus(ModerationStatus.CANCELLED);
        product.setModerationStatus(ModerationStatus.NOT_SUBMITTED);
    }

    @Transactional
    public void publishProduct(UUID productId, Long sellerId) {
        ProductEntity product = productRepository.findByPublicIdForUpdate(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerCanPublishProduct(product, sellerId);

        product.setStatus(ProductStatus.ACTIVE);

        log.info("Product was published: productId={}, sellerId={}", productId, sellerId);
    }

    @Transactional
    public void withdrawProductFromSale(UUID productId, Long sellerId) {
        ProductEntity product = productRepository.findByPublicIdForUpdate(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerOwnsProduct(product, sellerId);

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ProductStateException("Only active products can be withdrawn from sale");
        }

        product.setStatus(ProductStatus.OFF);
    }

    @Transactional
    public void archiveProduct(UUID productId, Long sellerId) {
        ProductEntity product = productRepository.findByPublicIdForUpdate(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerCanArchiveProduct(product, sellerId);

        product.setStatus(ProductStatus.ARCHIVED);
    }

    private void ensureSellerOwnsProduct(ProductEntity product, Long userId) {
        UserEntity seller = product.getSeller();

        if (userId == null || seller == null || !Objects.equals(seller.getId(), userId)
                || !seller.getRoles().contains(Role.SELLER)) {
            throw new AccessDeniedException("Only seller-owner can modify product: userId=" + userId
                    + " productId=" + product.getPublicId());
        }
    }

    private void ensureSellerCanEditProduct(ProductEntity product, Long userId) {
        ensureSellerOwnsProduct(product, userId);

        if (product.getStatus() != ProductStatus.OFF
                || product.getModerationStatus() == ModerationStatus.PENDING) {
            throw new ProductStateException("Only OFF products without pending moderation can be edited");
        }
    }

    private void deleteImageUnlessReferencedBySnapshot(String imageUrl) {
        if (!productModerationRepository.existsSnapshotReferencingImage(imageUrl)) {
            eventPublisher.publishEvent(new MediaImageDeletionRequested(imageUrl));
        }
    }

    private void ensureSellerCanSubmitProductForModeration(ProductEntity product, Long userId) {
        ensureSellerOwnsProduct(product, userId);

        if (product.getStatus() != ProductStatus.OFF
                || (product.getModerationStatus() != ModerationStatus.NOT_SUBMITTED
                && product.getModerationStatus() != ModerationStatus.REJECTED)) {
            throw new ProductStateException(
                    "Product cannot be submitted while it status is not OFF"
            );
        }

    }

    private void ensureSellerCanPublishProduct(ProductEntity product, Long userId) {
        ensureSellerOwnsProduct(product, userId);

        if (product.getStatus() != ProductStatus.OFF
                || product.getModerationStatus() != ModerationStatus.APPROVED) {
            throw new ProductStateException("Only approved OFF products can be published");
        }
    }

    private void ensureSellerCanArchiveProduct(ProductEntity product, Long userId) {
        ensureSellerOwnsProduct(product, userId);

        if (product.getStatus() != ProductStatus.OFF
                || product.getModerationStatus() == ModerationStatus.PENDING) {
            throw new ProductStateException(
                    "Only OFF products without pending moderation can be archived"
            );
        }
    }

    private void deleteAllProductDetailsImages(ProductEntity product){
        List<ProductDetailsImageEntity> images =
                new ArrayList<>(product.getDetailsImages());

        for (ProductDetailsImageEntity image : images) {
            mediaStorageService.deleteImage(image.getImageUrl());
            productDetailsImageRepository.delete(image);
        }
    }

    private void renumberImagePositions(List<ProductDetailsImageEntity> images) {
        int position = 0;
        for (ProductDetailsImageEntity image : images) {
            image.setPosition(position++);
        }
    }

    public ProductDetailsImageResponse toDetailsImageResponse(ProductDetailsImageEntity image) {
        return new ProductDetailsImageResponse(
                image.getPublicId(),
                image.getImageUrl(),
                image.getPosition()
        );
    }

    public ProductCardResponse toCardResponse(ProductCardEntity card) {
        return new ProductCardResponse(
                card.getProduct().getPublicId(),
                card.getProduct().getSlug(),
                card.getProduct().getName(),
                card.getProduct().getPrice(),
                card.getPreviewImageUrl(),
                card.getProduct().getStatus()
        );
    }

    public ProductDetailsResponse toDetailsResponse(ProductEntity product) {
        return new ProductDetailsResponse(
                product.getPublicId(),
                product.getName(),
                product.getSlug(),
                product.getPrice(),
                product.getDescription(),
                product.getDetailsImages().stream()
                        .map(image -> image.getImageUrl())
                        .toList(),
                productCardRepository.findByProduct_PublicId(product.getPublicId())
                        .map(ProductCardEntity::getPreviewImageUrl)
                        .orElse(null),
                product.getStatus(),
                product.getModerationStatus()
        );
    }

}
