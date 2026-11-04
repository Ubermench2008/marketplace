package ru.nsu.marketplace.media;

public class ImageNotFoundException extends RuntimeException {
    public ImageNotFoundException(String imgUrl) {
        super("Image not found: " + imgUrl);
    }
}
