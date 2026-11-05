package ru.nsu.marketplace.auth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import ru.nsu.marketplace.common.web.ApiErrorResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
class SecurityErrorWriter {
    private final ObjectMapper objectMapper;

    void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        ApiErrorResponse error = new ApiErrorResponse(status.value(), message, List.of(), Instant.now());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getOutputStream().write(objectMapper.writeValueAsBytes(error));
    }
}
