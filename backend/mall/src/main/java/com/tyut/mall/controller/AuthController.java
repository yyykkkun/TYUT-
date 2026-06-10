package com.tyut.mall.controller;

import com.tyut.mall.common.ApiResponse;
import com.tyut.mall.common.UserContext;
import com.tyut.mall.dto.request.AnonymousLoginRequest;
import com.tyut.mall.dto.request.LoginRequest;
import com.tyut.mall.dto.request.RegisterRequest;
import com.tyut.mall.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login/anonymous")
    public ApiResponse<?> anonymousLogin(@Valid @RequestBody AnonymousLoginRequest request) {
        return ApiResponse.ok(authService.anonymousLogin(request));
    }

    @GetMapping("/me")
    public ApiResponse<?> me() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return ApiResponse.fail(401, "未登录");
        }
        return ApiResponse.ok(authService.me(userId));
    }
}
