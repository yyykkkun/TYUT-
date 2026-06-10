package com.tyut.mall.controller;

import com.tyut.mall.common.ApiResponse;
import com.tyut.mall.common.UserContext;
import com.tyut.mall.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ApiResponse<?> list() {
        Long userId = getUserId();
        return ApiResponse.ok(messageService.list(userId));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<?> markRead(@PathVariable Long id) {
        Long userId = getUserId();
        messageService.markRead(userId, id);
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
