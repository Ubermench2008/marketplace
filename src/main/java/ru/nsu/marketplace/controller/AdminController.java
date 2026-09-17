package ru.nsu.marketplace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.nsu.marketplace.dto.CreateUserRequest;
import ru.nsu.marketplace.dto.UpdateUserRequest;
import ru.nsu.marketplace.dto.UserResponse;
import ru.nsu.marketplace.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService service;

    @GetMapping("/users")
    public Page<UserResponse> getUsers(
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return service.getUsers(pageable);
    }

    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable("id") Long userId) {
        return service.getUser(userId);
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createUser(request));
    }

    @PatchMapping("/users/{id}")
    public UserResponse updateUser(
            @PathVariable("id") Long userId,
            @RequestBody @Valid UpdateUserRequest request
    ) {
        return service.updateUser(userId, request);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long userId) {
        service.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
