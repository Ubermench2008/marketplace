package ru.nsu.marketplace.service;

import exceptions.ImageNotFoundException;
import exceptions.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.nsu.marketplace.dto.CreateProductRequest;
import ru.nsu.marketplace.dto.ProductCardResponse;
import ru.nsu.marketplace.entity.ProductCardEntity;
import ru.nsu.marketplace.entity.ProductEntity;
import ru.nsu.marketplace.repository.ProductCardsRepository;
import ru.nsu.marketplace.repository.ProductRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    public final ProductCardsRepository repository;
    public final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductCardResponse> getAll(Pageable pageable){
        return repository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductCardResponse getById(UUID id){
        ProductCardEntity entity = repository.findByPublicId(id).orElseThrow(
                () -> new ProductNotFoundException("Product not found")
        );

        return toResponse(entity);
    }

    @Transactional
    public ProductCardResponse createProduct(CreateProductRequest request){
        validateImgExist(request.imgUrl());

        ProductEntity product = new ProductEntity();
        product.setPublicId(UUID.randomUUID());
        product.setSlug(request.slug());
        product.setName(request.name());
        product.setPrice(request.price());
        product.setDescription(request.description());

        ProductEntity savedProduct = productRepository.save(product);

        ProductCardEntity entity = new ProductCardEntity();

        entity.setPublicId(UUID.randomUUID());
        entity.setImgUrl(request.imgUrl());
        entity.setProduct(savedProduct);

        ProductCardEntity saved = repository.save(entity);

        return toResponse(saved);
    }

    private ProductCardResponse toResponse(ProductCardEntity entity){
        return new ProductCardResponse(
                entity.getPublicId(),
                entity.getProduct().getSlug(),
                entity.getProduct().getName(),
                entity.getProduct().getPrice(),
                entity.getImgUrl()
        );
    }

    private void validateImgExist(String imgUrl){
        if (!imgUrl.startsWith("/media/")){
            throw new ImageNotFoundException(imgUrl);
        }

        Resource resource = new ClassPathResource("static" + imgUrl);

        if (!resource.exists()){
            throw new ImageNotFoundException(imgUrl);
        }
    }
}
