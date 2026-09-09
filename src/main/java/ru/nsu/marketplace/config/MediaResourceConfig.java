package ru.nsu.marketplace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {
    private final String mediaLocation;

    public MediaResourceConfig(@Value("${data.storage.location}") String mediaLocation) {
        this.mediaLocation = mediaLocation;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations(Path.of(mediaLocation).toUri().toString());
    }
}
