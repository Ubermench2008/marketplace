package ru.nsu.marketplace.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.marketplace.dto.ProductCardResponse;
import ru.nsu.marketplace.service.ProductService;

@RestController()
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {
    private final ProductService productService;

    @GetMapping("/products")
    public Page<ProductCardResponse> getProductCards(@PageableDefault(size = 20, sort = "product.name") Pageable pageable,
                                                     @AuthenticationPrincipal Long userId) {
        return productService.getCatalogBySeller(pageable, userId);
    }
}
