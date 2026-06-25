package com.tyut.mall.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JSONObject request = JSON.parseObject(message.getPayload());
        String conversationId = request.getString("conversationId");
        String content = request.getString("content");
        if (conversationId == null || content == null || content.isBlank()) {
            return;
        }

        JSONObject response = new JSONObject();
        response.put("id", "ws-" + System.currentTimeMillis());
        response.put("conversationId", conversationId);
        response.put("senderName", "卖家");
        response.put("senderRole", "seller");
        response.put("content", "收到：\"" + content + "\"。我会尽快确认成色、价格和交易地点。");
        response.put("createdAt", LocalDateTime.now().format(FORMATTER));
        response.put("self", false);
        session.sendMessage(new TextMessage(response.toJSONString()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }
}
