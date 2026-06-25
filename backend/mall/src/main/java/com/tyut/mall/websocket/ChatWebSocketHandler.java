package com.tyut.mall.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.tyut.mall.common.ApiException;
import com.tyut.mall.dto.response.ChatMessageVO;
import com.tyut.mall.security.JwtUtil;
import com.tyut.mall.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final String USER_ID_ATTR = "userId";

    private final JwtUtil jwtUtil;
    private final MessageService messageService;
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String token = getQueryParam(session, "token");
        if (token == null || !jwtUtil.validateToken(token)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未登录或 token 已过期"));
            return;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        session.getAttributes().put(USER_ID_ATTR, userId);
        userSessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long userId = (Long) session.getAttributes().get(USER_ID_ATTR);
        if (userId == null) {
            sendError(session, "连接未认证");
            return;
        }

        try {
            JSONObject request = JSON.parseObject(message.getPayload());
            Long conversationId = request.getLong("conversationId");
            String content = request.getString("content");
            if (conversationId == null || content == null || content.isBlank()) {
                sendError(session, "消息内容不能为空");
                return;
            }

            ChatMessageVO sent = messageService.sendChatMessage(userId, conversationId, content);
            Long messageId = Long.valueOf(sent.getId());
            for (Long participantId : messageService.getConversationParticipantIds(userId, conversationId)) {
                ChatMessageVO payload = messageService.getChatMessage(participantId, messageId);
                sendToUser(participantId, JSON.toJSONString(payload));
            }
        } catch (ApiException e) {
            sendError(session, e.getMessage());
        } catch (Exception e) {
            sendError(session, "消息发送失败");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get(USER_ID_ATTR);
        if (userId == null) return;

        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) {
            userSessions.remove(userId);
        }
    }

    private void sendToUser(Long userId, String payload) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) return;
        sessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : sessions) {
            try {
                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(payload));
                    }
                }
            } catch (IOException ignored) {
                // 下次发送前会清理关闭的连接
            }
        }
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        JSONObject payload = new JSONObject();
        payload.put("type", "error");
        payload.put("message", message);
        session.sendMessage(new TextMessage(payload.toJSONString()));
    }

    private String getQueryParam(WebSocketSession session, String name) {
        if (session.getUri() == null || session.getUri().getQuery() == null) return null;
        String[] pairs = session.getUri().getQuery().split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && name.equals(parts[0])) {
                return parts[1];
            }
        }
        return null;
    }
}
