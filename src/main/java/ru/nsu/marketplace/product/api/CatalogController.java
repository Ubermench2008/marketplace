package ru.nsu.marketplace.product.api;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import ru.nsu.marketplace.product.ProductService;
import ru.nsu.marketplace.product.dto.CatalogFilter;
import ru.nsu.marketplace.product.dto.ProductCardResponse;
import ru.nsu.marketplace.product.dto.ProductCategoryResponse;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/catalog")
public class CatalogController {
    private final ProductService productService;

    @GetMapping("/categories")
    public List<ProductCategoryResponse> getProductCategories() {
        return productService.getProductCategories();
    }

    @GetMapping("/products")
    public Page<ProductCardResponse> getProductCards(
        @ModelAttribute CatalogFilter filter,
        @PageableDefault(size = 20, sort = "product.name") Pageable pageable
    ) {
        return productService.getCatalogProductCards(filter, pageable);
    }
}
