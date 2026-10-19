package ru.nsu.marketplace.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import ru.nsu.marketplace.entity.*;
import ru.nsu.marketplace.enums.Role;
import ru.nsu.marketplace.exceptions.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.nsu.marketplace.dto.*;
import ru.nsu.marketplace.exceptions.UserNotFoundException;
import ru.nsu.marketplace.repository.*;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductCardsRepository productCardsRepository;
    private final ProductRepository productRepository;
    private final ProductDetailsImageRepository productDetailsImageRepository;
    private final UserRepository userRepository;
    private final MediaStorageService mediaStorageService;

    @Transactional(readOnly = true)
    public Page<ProductCardResponse> getCatalog(Pageable pageable){
        return productCardsRepository.findAll(pageable)
                .map(this::toCardResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductCardResponse> getCatalogBySeller(Pageable pageable, Long userId){
        return productCardsRepository.findAllByProduct_Seller_Id(userId, pageable)
                .map(this::toCardResponse);
    }

    @Transactional(readOnly = true)
    public ProductDetailsResponse getProduct(UUID id){
        ProductEntity entity = productRepository.findByPublicId(id).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        return toDetailsResponse(entity);
    }

    @Transactional
    public ProductDetailsResponse createProduct(CreateProductRequest request,
                                                MultipartFile previewImage,
                                                List<MultipartFile> detailsImages,
                                                Long userId) {
        if (detailsImages == null || detailsImages.isEmpty()) {
            throw new IllegalArgumentException("At least one details image is required");
        }

        UserEntity user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("Seller not found: userId=" + userId)
        );

        ProductEntity product = ProductEntity.builder()
                .publicId(UUID.randomUUID())
                .slug(request.slug())
                .name(request.name())
                .price(request.price())
                .description(request.description())
                .seller(user)
                .build();

        ensureSellerOwnsProduct(product, userId);

        ProductEntity savedProduct = productRepository.save(product);

        ProductCardEntity entity = new ProductCardEntity();

        entity.setImgUrl(mediaStorageService.saveImage(previewImage));
        entity.setProduct(savedProduct);

        productCardsRepository.save(entity);

        int positionIdx = 0;

        for (MultipartFile detailsImage : detailsImages) {
            ProductDetailsImageEntity imageEntity = ProductDetailsImageEntity.builder()
                    .publicId(UUID.randomUUID())
                    .product(savedProduct)
                    .imageUrl(mediaStorageService.saveImage(detailsImage))
                    .position(positionIdx++)
                    .build();

            productDetailsImageRepository.save(imageEntity);
            savedProduct.getDetailsImages().add(imageEntity);
        }
        log.info("Product was created: productId={}", savedProduct.getPublicId());

        return toDetailsResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public List<ImagesDetailsResponse> getImages(UUID productId) {
        ProductEntity product = productRepository.findByPublicId(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        return product.getDetailsImages().stream()
                .map(this::toImagesDetailsResponse)
                .toList();

    }

    @Transactional
    public List<ImagesDetailsResponse> addImages(UUID productId, List<MultipartFile> images, Long userId) {
        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerOwnsProduct(product, userId);

        List<ImagesDetailsResponse> response = new ArrayList<>();

        for (MultipartFile rawImage : images) {
            UUID uuid = UUID.randomUUID();
            String imageUrl = mediaStorageService.saveImage(rawImage);
            int position = product.getDetailsImages().size();

            ProductDetailsImageEntity entity = ProductDetailsImageEntity.builder()
                    .publicId(uuid)
                    .product(product)
                    .imageUrl(imageUrl)
                    .position(position)
                    .build();

            productDetailsImageRepository.save(entity);
            product.getDetailsImages().add(entity);
            response.add(toImagesDetailsResponse(entity));
        }

        List<UUID> addedImages = response.stream()
                .map(ImagesDetailsResponse::uuid)
                .toList();


        log.info("Product details images added: productId={}, imageIds={}",
                productId, addedImages);

        return response;
    }

    @Transactional
    public ProductDetailsResponse updateProduct(UUID productId, Long userId, UpdateProductRequest request) {
        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerOwnsProduct(product, userId);

        if (request.slug() != null) {
            product.setSlug(request.slug());
        }

        if (request.name() != null) {
            product.setName(request.name());
        }

        if (request.description() != null) {
            product.setDescription(request.description());
        }

        if (request.price() != null) {
            product.setPrice(request.price());
        }

        log.info("Product was updated: productId={}", productId);

        return toDetailsResponse(product);
    }

    @Transactional
    public List<ImagesDetailsResponse> updateImagesPositions(
            UUID productId,
            UpdateImagesPositionsRequest request,
            Long userId
    ) {
        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerOwnsProduct(product, userId);

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

        List<ImagesDetailsResponse> response = new ArrayList<>();
        for (int position = 0; position < imageIds.size(); position++) {
            ProductDetailsImageEntity image = imagesById.get(imageIds.get(position));
            image.setPosition(position);

            response.add(toImagesDetailsResponse(image));
        }


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
    public ImagesDetailsResponse deleteProductDetailsImage(UUID imageId, UUID productId, Long userId) {
        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerOwnsProduct(product, userId);

        ProductDetailsImageEntity image = product.getDetailsImages().stream()
                .filter(item -> item.getPublicId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Image does not belong to this product"
                ));

        ImagesDetailsResponse response = toImagesDetailsResponse(image);

        product.getDetailsImages().remove(image);
        renumberImagePositions(product.getDetailsImages());

        productDetailsImageRepository.delete(image);
        mediaStorageService.deleteImage(image.getImageUrl());

        log.info("Product details image was deleted: \n" +
                        "productId={} \n" +
                        "imageId={}",
                productId,
                imageId);

        return response;
    }

    @Transactional
    public ProductCardResponse updatePreview(UUID productId, MultipartFile preview, Long userId) {
        ProductCardEntity card = productCardsRepository
                .findByProduct_PublicId(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        ensureSellerOwnsProduct(card.getProduct(), userId);

        String oldImgUrl = card.getImgUrl();
        String newImgeUrl = mediaStorageService.saveImage(preview);

        card.setImgUrl(newImgeUrl);
        mediaStorageService.deleteImage(oldImgUrl);

        log.info("Product preview was updated: productId={}", productId);

        return toCardResponse(card);
    }

    @Transactional
    public void deleteProduct(UUID productId, Long userId) {
        ProductCardEntity card = productCardsRepository.findByProduct_PublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        ensureSellerOwnsProduct(product, userId);

        String previewUrl = card.getImgUrl();

        mediaStorageService.deleteImage(previewUrl);

        productCardsRepository.delete(card);

        deleteAllProductDetailsImages(product);

        productRepository.delete(product);

        log.info("Product was deleted: productId={}", productId);
    }

    private void ensureSellerOwnsProduct(ProductEntity product, Long userId) {
        UserEntity seller = product.getSeller();

        if (userId == null || seller == null || !Objects.equals(seller.getId(), userId)
                || !seller.getRoles().contains(Role.SELLER)) {
            throw new AccessDeniedException("Only seller-owner can modify product: userId=" + userId
                    + " productId=" + product.getPublicId());
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
        int i = 0;
        for (ProductDetailsImageEntity image : images) {
            image.setPosition(i++);
        }
    }

    public ImagesDetailsResponse toImagesDetailsResponse(ProductDetailsImageEntity image) {
        return new ImagesDetailsResponse(
                image.getPublicId(),
                image.getImageUrl(),
                image.getPosition()
        );
    }

    public ProductCardResponse toCardResponse(ProductCardEntity entity) {
        return new ProductCardResponse(
                entity.getProduct().getPublicId(),
                entity.getProduct().getSlug(),
                entity.getProduct().getName(),
                entity.getProduct().getPrice(),
                entity.getImgUrl()
        );
    }

    public ProductDetailsResponse toDetailsResponse(ProductEntity entity) {
        return new ProductDetailsResponse(
                entity.getPublicId(),
                entity.getName(),
                entity.getSlug(),
                entity.getPrice(),
                entity.getDescription(),
                entity.getDetailsImages().stream()
                        .map(image -> image.getImageUrl())
                        .toList()
        );
    }

}
