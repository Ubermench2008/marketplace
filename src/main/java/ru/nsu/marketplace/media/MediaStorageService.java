package ru.nsu.marketplace.media;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

@Service
@Slf4j
public class MediaStorageService {
    @Value("${data.storage.location}")
    private String storageLocation;

    public String saveImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        String extension = getImageExtension(image.getContentType());
        UUID fileId = UUID.randomUUID();
        Path directory = Path.of(storageLocation);
        Path path = directory.resolve(fileId + extension);

        try (InputStream input = image.getInputStream()) {
            Files.createDirectories(directory);
            Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            log.error("Could not save image: path={}", path, exception);

            throw new IllegalStateException("Could not save image", exception);
        }

        return "/media/" + fileId + extension;
    }

    public boolean hasSameContent(String imageUrl, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        getImageExtension(image.getContentType());
        Path path = resolveImagePath(imageUrl);
        if (!Files.isRegularFile(path)) {
            return false;
        }

        try (InputStream stored = Files.newInputStream(path);
             InputStream uploaded = image.getInputStream()) {
            while (true) {
                byte[] storedBytes = stored.readNBytes(8192);
                byte[] uploadedBytes = uploaded.readNBytes(8192);
                if (!Arrays.equals(storedBytes, uploadedBytes)) {
                    return false;
                }
                if (storedBytes.length == 0) {
                    return true;
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not compare image contents", exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deleteImageAfterCommit(MediaImageDeletionRequested event) {
        try {
            deleteImage(event.imageUrl());
        } catch (RuntimeException exception) {
            log.error("Could not delete image after commit: imageUrl={}", event.imageUrl(), exception);
        }
    }

    public void deleteImage(String imageUrl) {
        Path path = resolveImagePath(imageUrl);

        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.error("Could not delete image: path={}", path, exception);

            throw new IllegalStateException("Could not delete image", exception);
        }
    }

    private Path resolveImagePath(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/media/")) {
            throw new IllegalArgumentException("Invalid image URL");
        }

        Path fileName = Path.of(imageUrl).getFileName();
        return Path.of(storageLocation).resolve(fileName).normalize();
    }

    private String getImageExtension(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("Image content type is required");
        }
        return switch (contentType) {
            case "image/webp" -> ".webp";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new IllegalArgumentException("Only WEBP, JPEG and PNG images are supported");
        };
    }
}
