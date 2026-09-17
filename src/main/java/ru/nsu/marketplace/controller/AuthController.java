package ru.nsu.marketplace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.marketplace.dto.LoginRequest;
import ru.nsu.marketplace.dto.LoginResponse;
import ru.nsu.marketplace.dto.RegisterRequest;
import ru.nsu.marketplace.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        String accessToken = service.login(request);
        return new LoginResponse(accessToken);
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        service.register(request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
