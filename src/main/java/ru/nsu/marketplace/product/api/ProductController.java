package ru.nsu.marketplace.product.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.nsu.marketplace.product.ProductService;
import ru.nsu.marketplace.product.dto.CreateProductRequest;
import ru.nsu.marketplace.product.dto.ProductCardResponse;
import ru.nsu.marketplace.product.dto.ProductDetailsImageResponse;
import ru.nsu.marketplace.product.dto.ProductDetailsResponse;
import ru.nsu.marketplace.product.dto.UpdateProductImageOrderRequest;
import ru.nsu.marketplace.product.dto.UpdateProductRequest;

import java.util.List;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping("/{id}")
    public ProductDetailsResponse getProduct(@PathVariable("id") UUID productId) {
        return productService.getProduct(productId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductDetailsResponse> createProduct(
            @RequestPart("product") @Valid CreateProductRequest product,
            @RequestPart("previewImage") MultipartFile previewImage,
            @RequestPart("detailsImages") List<MultipartFile> detailsImages,
            @AuthenticationPrincipal Long userId
    ) {
        ProductDetailsResponse response = productService.createProduct(product, previewImage, detailsImages, userId);
        return ResponseEntity.created(URI.create("/api/products/" + response.id())).body(response);
    }

    @GetMapping("/{id}/details-images")
    public List<ProductDetailsImageResponse> getProductDetailsImages(@PathVariable("id") UUID productId) {
        return productService.getProductDetailsImages(productId);
    }

    @PostMapping(value = "/{id}/details-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<ProductDetailsImageResponse>> addProductDetailsImages(
            @PathVariable("id") UUID productId,
            @RequestPart("images") List<MultipartFile> images,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.addProductDetailsImages(productId, images, userId));
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
    public List<ProductDetailsImageResponse> updateProductImageOrder(
            @PathVariable("id") UUID productId,
            @RequestBody @Valid UpdateProductImageOrderRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        return productService.updateProductImageOrder(productId, request, userId);
    }

    @DeleteMapping("/{id}/details-images/{imageId}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> deleteProductDetailsImage(
            @PathVariable("id") UUID productId,
            @PathVariable UUID imageId,
            @AuthenticationPrincipal Long userId
    ) {
        productService.deleteProductDetailsImage(imageId, productId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{id}/preview-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ProductCardResponse updateProductPreview(
            @PathVariable("id") UUID productId,
            @RequestPart("previewImage") MultipartFile preview,
            @AuthenticationPrincipal Long userId
    ) {
        return productService.updateProductPreview(productId, preview, userId);
    }

    @PostMapping("/{productId}/moderation-requests")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> submitProductForModeration(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Long sellerId
    ) {
        productService.submitProductForModeration(productId, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{productId}/moderation-requests/cancel")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> cancelProductModerationRequest(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Long sellerId
    ) {
        productService.cancelProductModerationRequest(productId, sellerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/publish")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> publishProduct(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Long sellerId
    ) {
        productService.publishProduct(productId, sellerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/withdraw")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> withdrawProductFromSale(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Long sellerId
    ) {
        productService.withdrawProductFromSale(productId, sellerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/archive")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> archiveProduct(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Long sellerId
    ) {
        productService.archiveProduct(productId, sellerId);
        return ResponseEntity.noContent().build();
    }
}
