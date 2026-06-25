package com.tyut.mall.controller;

import com.tyut.mall.common.ApiResponse;
import com.tyut.mall.common.UserContext;
import com.tyut.mall.dto.request.CreateConversationRequest;
import com.tyut.mall.dto.request.SendChatMessageRequest;
import com.tyut.mall.service.MessageService;
import jakarta.validation.Valid;
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

    @GetMapping("/conversations")
    public ApiResponse<?> listConversations() {
        Long userId = getUserId();
        return ApiResponse.ok(messageService.listConversations(userId));
    }

    @PostMapping("/conversations")
    public ApiResponse<?> createConversation(@Valid @RequestBody CreateConversationRequest request) {
        Long userId = getUserId();
        return ApiResponse.ok(messageService.createConversation(userId, request.getProductId()));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<?> listChatMessages(@PathVariable Long id) {
        Long userId = getUserId();
        return ApiResponse.ok(messageService.listChatMessages(userId, id));
    }

    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<?> sendChatMessage(@PathVariable Long id, @Valid @RequestBody SendChatMessageRequest request) {
        Long userId = getUserId();
        return ApiResponse.ok(messageService.sendChatMessage(userId, id, request.getContent()));
    }

    private Long getUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        return userId;
    }
}
