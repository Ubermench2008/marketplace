package ru.nsu.marketplace.service;

import exceptions.ImageNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ru.nsu.marketplace.dto.CreateProductRequest;
import ru.nsu.marketplace.dto.ProductCardResponse;
import ru.nsu.marketplace.entity.ProductCardEntity;
import ru.nsu.marketplace.repository.ProductCardsRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    public final ProductCardsRepository repository;

    public List<ProductCardResponse> getAll(){
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductCardResponse getById(UUID id){
        ProductCardEntity entity = repository.findByPublicId(id).orElseThrow(
                () -> new RuntimeException("Product not found")
        );

        return toResponse(entity);
    }

    public ProductCardResponse createProductCard(CreateProductRequest request){
        validateImgExist(request.imgUrl());
        ProductCardEntity entity = new ProductCardEntity();

        entity.setPublicId(UUID.randomUUID());
        entity.setSlug(request.slug());
        entity.setName(request.name());
        entity.setPrice(request.price());
        entity.setImgUrl(request.imgUrl());

        ProductCardEntity saved = repository.save(entity);

        return toResponse(saved);
    }

    private ProductCardResponse toResponse(ProductCardEntity entity){
        return new ProductCardResponse(
                entity.getPublicId(),
                entity.getSlug(),
                entity.getName(),
                entity.getPrice(),
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
