package ru.nsu.marketplace.product.stock.exception;

public class ProductStockNotFoundException extends RuntimeException {
    public ProductStockNotFoundException(String message) {
        super(message);
    }
}
