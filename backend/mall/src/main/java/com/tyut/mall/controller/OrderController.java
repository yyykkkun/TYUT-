package com.tyut.mall.controller;

import com.tyut.mall.common.ApiResponse;
import com.tyut.mall.common.UserContext;
import com.tyut.mall.dto.request.OrderPreviewRequest;
import com.tyut.mall.dto.request.ReviewRequest;
import com.tyut.mall.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false, defaultValue = "all") String status,
                                @RequestParam(required = false, defaultValue = "1") Integer page,
                                @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        Long userId = getUserId();
        return ApiResponse.ok(orderService.list(userId, status, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        Long userId = getUserId();
        return ApiResponse.ok(orderService.detail(userId, id));
    }

    @PostMapping("/preview")
    public ApiResponse<?> preview(@RequestBody OrderPreviewRequest request) {
        Long userId = getUserId();
        return ApiResponse.ok(orderService.preview(userId, request));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody OrderPreviewRequest request) {
        Long userId = getUserId();
        String orderNo = orderService.create(userId, request);
        return ApiResponse.ok(orderNo);
    }

    @PutMapping("/{id}/pay")
    public ApiResponse<?> pay(@PathVariable Long id) {
        Long userId = getUserId();
        orderService.pay(userId, id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<?> cancel(@PathVariable Long id) {
        Long userId = getUserId();
        orderService.cancel(userId, id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<?> confirm(@PathVariable Long id) {
        Long userId = getUserId();
        orderService.confirm(userId, id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/review")
    public ApiResponse<?> review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        Long userId = getUserId();
        orderService.review(userId, id, request.getReview());
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
