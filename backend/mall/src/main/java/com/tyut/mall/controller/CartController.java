package com.tyut.mall.controller;

import com.tyut.mall.common.ApiResponse;
import com.tyut.mall.common.UserContext;
import com.tyut.mall.dto.request.AddCartRequest;
import com.tyut.mall.dto.request.SelectCartRequest;
import com.tyut.mall.dto.request.UpdateCartRequest;
import com.tyut.mall.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<?> list() {
        Long userId = getUserId();
        return ApiResponse.ok(cartService.list(userId));
    }

    @PostMapping
    public ApiResponse<?> add(@Valid @RequestBody AddCartRequest request) {
        Long userId = getUserId();
        cartService.add(userId, request);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<?> updateQuantity(@PathVariable Long id,
                                          @Valid @RequestBody UpdateCartRequest request) {
        Long userId = getUserId();
        cartService.updateQuantity(userId, id, request.getQuantity());
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/select")
    public ApiResponse<?> toggleSelect(@PathVariable Long id) {
        Long userId = getUserId();
        cartService.toggleSelect(userId, id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> remove(@PathVariable Long id) {
        Long userId = getUserId();
        cartService.remove(userId, id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/selected")
    public ApiResponse<?> clearSelected() {
        Long userId = getUserId();
        cartService.clearSelected(userId);
        return ApiResponse.ok();
    }

    private Long getUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        return userId;
    }
}
