package ru.nsu.marketplace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @PreAuthorize("hasRole('SELLER')")
    public ProductDetailsResponse createProduct(
            @RequestPart("product") @Valid CreateProductRequest product,
            @RequestPart("previewImage") MultipartFile previewImage,
            @RequestPart("detailsImages") List<MultipartFile> detailsImages,
            @AuthenticationPrincipal Long userId) {
        return productService.createProduct(product, previewImage, detailsImages, userId);
    }

    @GetMapping("/{id}/details-images")
    public List<ImagesDetailsResponse> getImages(@PathVariable("id") UUID productId) {
        return productService.getImages(productId);
    }

    @PostMapping(value = "/{id}/details-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public List<ImagesDetailsResponse> addImages(
            @PathVariable("id") UUID productId,
            @RequestPart("images") List<MultipartFile> images,
            @AuthenticationPrincipal Long userId
    ) {
        return productService.addImages(productId, images, userId);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ProductDetailsResponse updateProduct(
            @PathVariable("id") UUID productId,
            @RequestBody @Valid UpdateProductRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        return productService.updateProduct(productId, userId, request);
    }

    @PatchMapping("/{id}/details-images/positions")
    @PreAuthorize("hasRole('SELLER')")
    public List<ImagesDetailsResponse> updateImagesPositions(
            @PathVariable("id") UUID productId,
            @RequestBody @Valid UpdateImagesPositionsRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        return productService.updateImagesPositions(productId, request, userId);
    }

    @DeleteMapping("/{id}/details-images/{imageId}")
    @PreAuthorize("hasRole('SELLER')")
    public ImagesDetailsResponse deleteProductDetailsImage(@PathVariable("id") UUID productId,
                                                   @PathVariable UUID imageId,
                                                   @AuthenticationPrincipal Long userId) {
        return productService.deleteProductDetailsImage(imageId, productId, userId);
    }

    @PutMapping(value = "/{id}/preview-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ProductCardResponse updatePreview(@PathVariable("id") UUID productId,
                                             @RequestPart("previewImage") MultipartFile preview,
                                             @AuthenticationPrincipal Long userId) {
        return productService.updatePreview(productId, preview, userId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") UUID productId,
                                            @AuthenticationPrincipal Long userId) {
        productService.deleteProduct(productId, userId);
        return ResponseEntity.noContent().build();
    }

}
