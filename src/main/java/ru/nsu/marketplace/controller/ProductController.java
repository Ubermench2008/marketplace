package ru.nsu.marketplace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import ru.nsu.marketplace.dto.CreateProductRequest;
import ru.nsu.marketplace.dto.ProductCardResponse;
import ru.nsu.marketplace.service.ProductService;

import java.util.UUID;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {
    public final ProductService productService;

    @GetMapping("/all")
    public Page<ProductCardResponse> getProduct(@PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return productService.getAll(pageable);
    }

    @GetMapping("/{id}")
    public ProductCardResponse getProduct(@PathVariable UUID id) {
        return productService.getById(id);
    }

    @PostMapping("/create")
    public ProductCardResponse createProduct(@RequestBody @Valid CreateProductRequest request) { return productService.createProduct(request); }
}
