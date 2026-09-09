package ru.nsu.marketplace.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class MediaStorageService {
    @Value("${data.storage.location}")
    private String mediaPath;

    public String saveImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        String extension = getExtension(image.getContentType());
        UUID fileId = UUID.randomUUID();
        Path directory = Path.of(mediaPath);
        Path path = directory.resolve(fileId + extension);

        try {
            Files.createDirectories(directory);
            Files.copy(image.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save image", exception);
        }

        return "/media/" + fileId + extension;
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/media/")) {
            throw new IllegalArgumentException("Invalid image URL");
        }

        Path fileName = Path.of(imageUrl).getFileName();
        Path path = Path.of(mediaPath).resolve(fileName).normalize();

        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete image", exception);
        }
    }

    private String getExtension(String contentType) {
        return switch (contentType) {
            case "image/webp" -> ".webp";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new IllegalArgumentException("Only WEBP, JPEG and PNG images are supported");
        };
    }
}
