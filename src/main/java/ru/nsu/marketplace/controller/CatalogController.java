package ru.nsu.marketplace.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.marketplace.service.ProductService;
import ru.nsu.marketplace.dto.ProductCardResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/catalog/products")
public class CatalogController {
    private final ProductService service;

    @GetMapping
    public Page<ProductCardResponse> getProductCards(
            @PageableDefault(size = 20, sort = "product.name") Pageable pageable
    ) {
        return service.getCatalog(pageable);
    }
}
