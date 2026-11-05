package ru.nsu.marketplace.product.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.marketplace.product.exception.ProductStateException;
import ru.nsu.marketplace.product.model.ProductEntity;
import ru.nsu.marketplace.product.model.ProductStatus;
import ru.nsu.marketplace.product.stock.exception.ProductStockNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductStockService {
    private final ProductStockRepository productStockRepository;

    @Transactional(readOnly = true)
    public List<ProductStockResponse> getProductStocks(List<UUID> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }

        return productStockRepository.findStocksByProductPublicIdsAndStatus(productIds, ProductStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public int getProductStock(Long productId) {
        ProductStockEntity stockEntity = productStockRepository.findById(productId).orElseThrow(
            () -> new ProductStockNotFoundException("Product stock not found")
        );

        return stockEntity.getQuantity();
    }

    @Transactional(readOnly = true)
    public ProductStockResponse getSellerProductStock(UUID productId, Long sellerId) {
        return productStockRepository.findStockByProductPublicIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new ProductStockNotFoundException("Product stock not found"));
    }

    @Transactional
    public ProductStockResponse updateSellerProductStock(UUID productId, Long sellerId, int delta) {
        if (delta == 0) {
            throw new IllegalArgumentException("Stock adjustment must not be zero");
        }

        ProductStockEntity stock = findSellerStockForUpdate(productId, sellerId);

        long quantity = (long) stock.getQuantity() + delta;
        if (quantity < 0) {
            throw new ProductStateException("Stock adjustment exceeds available quantity");
        }
        if (quantity > Integer.MAX_VALUE) {
            throw new ProductStateException("Stock adjustment exceeds maximum quantity");
        }

        stock.setQuantity((int) quantity);
        return new ProductStockResponse(productId, stock.getQuantity());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void createStockItem(ProductEntity product, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Initial quantity must be zero or greater");
        }

        ProductStockEntity stock = new ProductStockEntity(null, product, quantity);
        productStockRepository.save(stock);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public ProductStockEntity getProductStockEntityForUpdate(Long productId) {
        return productStockRepository.findByProductIdForUpdate(productId).orElseThrow(
            () -> new ProductStockNotFoundException("Product stock not found")
        );
    }

    private ProductStockEntity findSellerStockForUpdate(UUID productId, Long sellerId) {
        return productStockRepository.findSellerStockForUpdate(productId, sellerId)
                .orElseThrow(() -> new ProductStockNotFoundException("Product stock not found"));
    }
}
