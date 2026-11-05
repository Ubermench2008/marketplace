package ru.nsu.marketplace.product.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.marketplace.product.ProductService;
import ru.nsu.marketplace.product.dto.ProductCardResponse;
import ru.nsu.marketplace.product.stock.ProductStockResponse;
import ru.nsu.marketplace.product.stock.ProductStockService;
import ru.nsu.marketplace.product.stock.UpdateProductStockRequest;

import java.util.UUID;

@RestController
@PreAuthorize("hasRole('SELLER')")
@RequestMapping("/api/seller/products")
@RequiredArgsConstructor
public class SellerController {
    private final ProductService productService;
    private final ProductStockService productStockService;

    @GetMapping
    public Page<ProductCardResponse> getProductCards(
            @PageableDefault(size = 20, sort = "product.name") Pageable pageable,
            @AuthenticationPrincipal Long userId
    ) {
        return productService.getSellerProductCards(pageable, userId);
    }

    @GetMapping("/{productId}/stock")
    public ProductStockResponse getProductStock(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Long sellerId
    ) {
        return productStockService.getSellerProductStock(productId, sellerId);
    }

    @PatchMapping("/{productId}/stock")
    public ProductStockResponse updateProductStock(
            @PathVariable UUID productId,
            @AuthenticationPrincipal Long sellerId,
            @RequestBody @Valid UpdateProductStockRequest request
    ) {
        return productStockService.updateSellerProductStock(productId, sellerId, request.delta());
    }
}
