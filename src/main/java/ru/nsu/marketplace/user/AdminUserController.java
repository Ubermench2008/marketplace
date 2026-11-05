package ru.nsu.marketplace.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.nsu.marketplace.user.dto.CreateUserRequest;
import ru.nsu.marketplace.user.dto.UpdateUserRequest;
import ru.nsu.marketplace.user.dto.UserResponse;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserService userService;

    @GetMapping
    public Page<UserResponse> getUsers(
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return userService.getUsers(pageable);
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable Long userId) {
        return userService.getUser(userId);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.created(URI.create("/api/admin/users/" + user.id())).body(user);
    }

    @PatchMapping("/{userId}")
    public UserResponse updateUser(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserRequest request
    ) {
        return userService.updateUser(userId, request);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
