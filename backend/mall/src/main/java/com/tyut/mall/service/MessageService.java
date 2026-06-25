package com.tyut.mall.service;

import com.tyut.mall.dto.response.ChatMessageVO;
import com.tyut.mall.dto.response.ConversationVO;
import com.tyut.mall.dto.response.MessageVO;

import java.util.List;
import java.util.Set;

public interface MessageService {
    List<MessageVO> list(Long userId);
    void markRead(Long userId, Long messageId);
    List<ConversationVO> listConversations(Long userId);
    ConversationVO createConversation(Long userId, Long productId);
    List<ChatMessageVO> listChatMessages(Long userId, Long conversationId);
    ChatMessageVO sendChatMessage(Long userId, Long conversationId, String content);
    ChatMessageVO getChatMessage(Long userId, Long messageId);
    Set<Long> getConversationParticipantIds(Long userId, Long conversationId);
}
