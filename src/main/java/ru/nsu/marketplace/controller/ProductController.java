package ru.nsu.marketplace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.nsu.marketplace.dto.*;
import ru.nsu.marketplace.service.ProductService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    public final ProductService productService;

    @GetMapping("/{id}")
    public ProductDetailsResponse getProduct(@PathVariable("id") UUID productId) {
        return productService.getProduct(productId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductDetailsResponse createProduct(
            @RequestPart("product") @Valid CreateProductRequest product,
            @RequestPart("previewImage") MultipartFile previewImage,
            @RequestPart("detailsImages") List<MultipartFile> detailsImages) {
        return productService.createProduct(product, previewImage, detailsImages);
    }

    @GetMapping("/{id}/details-images")
    public List<ImagesDetailsResponse> getImages(@PathVariable("id") UUID productId) {
        return productService.getImages(productId);
    }

    @PostMapping(value = "/{id}/details-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ImagesDetailsResponse> addImages(
            @PathVariable("id") UUID productId,
            @RequestPart("images") List<MultipartFile> images
    ) {
        return productService.addImages(productId, images);
    }

    @PatchMapping("/{id}")
    public ProductDetailsResponse updateProduct(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateProductRequest request
    ) {
        return productService.updateProduct(id, request);
    }

    @PatchMapping("/{id}/details-images/positions")
    public List<ImagesDetailsResponse> updateImagesPositions(
            @PathVariable("id") UUID productId,
            @RequestBody @Valid UpdateImagesPositionsRequest request
    ) {
        return productService.updateImagesPositions(productId, request);
    }

    @DeleteMapping("/{id}/details-images/{imageId}")
    public ImagesDetailsResponse deleteProductDetailsImage(@PathVariable("id") UUID productId,
                                                   @PathVariable UUID imageId) {
        return productService.deleteProductDetailsImage(imageId, productId);
    }

    @PutMapping(value = "/{id}/preview-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductCardResponse updatePreview(@PathVariable("id") UUID productId,
                                             @RequestPart("previewImage") MultipartFile preview) {
        return productService.updatePreview(productId, preview);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") UUID productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
