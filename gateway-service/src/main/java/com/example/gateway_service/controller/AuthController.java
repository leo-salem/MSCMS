package com.example.gateway_service.controller;

import com.example.gateway_service.dto.*;
import com.example.gateway_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, signup, token refresh, and logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with username and password to get JWT tokens")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        try {
            Map<String, Object> tokens = authService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(tokens);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }

    @PostMapping("/signup")
    @Operation(summary = "Sign up as Fan", description = "Register a new account with FAN role (public)")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupRequest request) {
        try {
            Map<String, Object> result = authService.signup(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get a new access token using a refresh token")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshRequest request) {
        try {
            Map<String, Object> tokens = authService.refresh(request.getRefreshToken());
            return ResponseEntity.ok(tokens);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate the current session")
    public ResponseEntity<Map<String, Object>> logout(@Valid @RequestBody LogoutRequest request) {
        try {
            authService.logout(request.getRefreshToken());
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Logout failed"));
        }
    }

    @PostMapping("/admin/create-user")
    @Operation(summary = "Admin: Create user with role", description = "Create a new user with a specified role (ADMIN only)")
    public ResponseEntity<Map<String, Object>> adminCreateUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Map<String, Object> result = authService.adminCreateUser(request, authHeader);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
