package com.trackpro.controller;

import com.trackpro.dto.auth.AuthResponse;
import com.trackpro.dto.auth.LoginRequest;
import com.trackpro.dto.auth.LogoutRequest;
import com.trackpro.dto.auth.RefreshRequest;
import com.trackpro.service.AuthService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.email(), req.password(), Instant.now());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken(), Instant.now());
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody LogoutRequest req) {
        authService.logout(req.refreshToken(), Instant.now());
    }
}
