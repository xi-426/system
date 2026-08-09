package com.yan.campuspass.auth.controller;

import com.yan.campuspass.auth.dto.LoginRequest;
import com.yan.campuspass.auth.dto.LoginResponse;
import com.yan.campuspass.auth.service.AuthService;
import com.yan.campuspass.common.api.ApiResponse;
import com.yan.campuspass.security.CurrentUser;
import com.yan.campuspass.security.TokenBlacklistService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(
            AuthService authService,
            TokenBlacklistService tokenBlacklistService) {
        this.authService = authService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal CurrentUser currentUser) {
        tokenBlacklistService.blacklist(currentUser);
        return ApiResponse.ok(null);
    }
}
