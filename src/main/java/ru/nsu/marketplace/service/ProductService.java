package ru.nsu.marketplace.service;

import exceptions.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.nsu.marketplace.dto.*;
import ru.nsu.marketplace.entity.ProductCardEntity;
import ru.nsu.marketplace.entity.ProductDetailsImageEntity;
import ru.nsu.marketplace.entity.ProductEntity;
import ru.nsu.marketplace.repository.ProductCardsRepository;
import ru.nsu.marketplace.repository.ProductDetailsImageRepository;
import ru.nsu.marketplace.repository.ProductRepository;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    public final ProductCardsRepository productCardsRepository;
    public final ProductRepository productRepository;
    public final ProductDetailsImageRepository productDetailsImageRepository;
    public final MediaStorageService mediaStorageService;

    @Transactional(readOnly = true)
    public Page<ProductCardResponse> getCatalog(Pageable pageable){
        return productCardsRepository.findAll(pageable)
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
                                                List<MultipartFile> detailsImages) {
        if (detailsImages == null || detailsImages.isEmpty()) {
            throw new IllegalArgumentException("At least one details image is required");
        }

        ProductEntity product = new ProductEntity();
        product.setPublicId(UUID.randomUUID());
        product.setSlug(request.slug());
        product.setName(request.name());
        product.setPrice(request.price());
        product.setDescription(request.description());

        ProductEntity savedProduct = productRepository.save(product);

        ProductCardEntity entity = new ProductCardEntity();

        entity.setImgUrl(mediaStorageService.saveImage(previewImage));
        entity.setProduct(savedProduct);

        productCardsRepository.save(entity);

        int positionIdx = 0;

        for (MultipartFile detailsImage : detailsImages) {
            ProductDetailsImageEntity imageEntity = new ProductDetailsImageEntity();
            imageEntity.setPublicId(UUID.randomUUID());
            imageEntity.setProduct(savedProduct);
            imageEntity.setImageUrl(mediaStorageService.saveImage(detailsImage));
            imageEntity.setPosition(positionIdx++);

            productDetailsImageRepository.save(imageEntity);
            savedProduct.getDetailsImages().add(imageEntity);
        }

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
    public List<ImagesDetailsResponse> addImages(UUID productId, List<MultipartFile> images) {
        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        List<ImagesDetailsResponse> response = new ArrayList<>();

        for (MultipartFile rawImage : images) {
            ProductDetailsImageEntity entity = new ProductDetailsImageEntity();

            UUID uuid = UUID.randomUUID();
            String imageUrl = mediaStorageService.saveImage(rawImage);
            int position = product.getDetailsImages().size();

            entity.setPublicId(uuid);
            entity.setProduct(product);
            entity.setImageUrl(imageUrl);
            entity.setPosition(position);

            productDetailsImageRepository.save(entity);
            product.getDetailsImages().add(entity);
            response.add(toImagesDetailsResponse(entity));
        }

        return response;
    }

    @Transactional
    public ProductDetailsResponse updateProduct(UUID productId, UpdateProductRequest request) {
        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

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

        return toDetailsResponse(product);
    }

    @Transactional
    public List<ImagesDetailsResponse> updateImagesPositions(
            UUID productId,
            UpdateImagesPositionsRequest request
    ) {
        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

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

        List<ImagesDetailsResponse> response = new ArrayList<>();
        for (int position = 0; position < imageIds.size(); position++) {
            ProductDetailsImageEntity image = imagesById.get(imageIds.get(position));
            image.setPosition(position);

            response.add(toImagesDetailsResponse(image));
        }

        return response;
    }

    @Transactional
    public ImagesDetailsResponse deleteProductDetailsImage(UUID imageId, UUID productId) {
        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

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

        return response;
    }

    @Transactional
    public ProductCardResponse updatePreview(UUID productId, MultipartFile preview) {
        ProductCardEntity card = productCardsRepository
                .findByProduct_PublicId(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        String oldImgUrl = card.getImgUrl();
        String newImgeUrl = mediaStorageService.saveImage(preview);

        card.setImgUrl(newImgeUrl);
        mediaStorageService.deleteImage(oldImgUrl);

        return toCardResponse(card);
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        ProductCardEntity card = productCardsRepository.findByProduct_PublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        String previewUrl = card.getImgUrl();

        mediaStorageService.deleteImage(previewUrl);

        productCardsRepository.delete(card);

        ProductEntity product = productRepository.findByPublicId(productId).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        deleteAllProductDetailsImages(product);

        productRepository.delete(product);
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

    private ImagesDetailsResponse toImagesDetailsResponse(ProductDetailsImageEntity image) {
        return new ImagesDetailsResponse(
                image.getPublicId(),
                image.getImageUrl(),
                image.getPosition()
        );
    }

    private ProductCardResponse toCardResponse(ProductCardEntity entity) {
        return new ProductCardResponse(
                entity.getProduct().getPublicId(),
                entity.getProduct().getSlug(),
                entity.getProduct().getName(),
                entity.getProduct().getPrice(),
                entity.getImgUrl()
        );
    }

    private ProductDetailsResponse toDetailsResponse(ProductEntity entity) {
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
