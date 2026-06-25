package com.tyut.mall.service.impl;

import com.tyut.mall.common.ApiException;
import com.tyut.mall.dto.response.ChatMessageVO;
import com.tyut.mall.dto.response.ConversationVO;
import com.tyut.mall.dto.response.MessageVO;
import com.tyut.mall.entity.ChatConversation;
import com.tyut.mall.entity.ChatMessage;
import com.tyut.mall.entity.Product;
import com.tyut.mall.entity.User;
import com.tyut.mall.repository.ChatConversationRepository;
import com.tyut.mall.repository.ChatMessageRepository;
import com.tyut.mall.repository.MessageRepository;
import com.tyut.mall.repository.ProductRepository;
import com.tyut.mall.repository.UserRepository;
import com.tyut.mall.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MessageRepository messageRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public List<MessageVO> list(Long userId) {
        return messageRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(m -> MessageVO.builder()
                        .id(String.valueOf(m.getId()))
                        .title(m.getTitle())
                        .body(m.getBody())
                        .type(m.getType())
                        .read(m.getRead() == 1)
                        .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void markRead(Long userId, Long messageId) {
        var message = messageRepository.findById(messageId)
                .orElseThrow(() -> ApiException.notFound("消息不存在"));
        if (!message.getUserId().equals(userId)) {
            throw ApiException.badRequest("无权操作");
        }
        message.setRead(1);
        messageRepository.save(message);
    }

    @Override
    public List<ConversationVO> listConversations(Long userId) {
        return chatConversationRepository.findByBuyerIdOrSellerIdOrderByUpdatedAtDesc(userId, userId)
                .stream()
                .map(conversation -> toConversationVO(conversation, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConversationVO createConversation(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("商品不存在"));
        Long sellerId = resolveSellerId(userId, product);

        ChatConversation conversation = chatConversationRepository
                .findByProductIdAndBuyerIdAndSellerId(productId, userId, sellerId)
                .orElseGet(() -> chatConversationRepository.save(ChatConversation.builder()
                        .productId(productId)
                        .buyerId(userId)
                        .sellerId(sellerId)
                        .lastMessage("我想了解一下这件闲置。")
                        .buyerUnread(0)
                        .sellerUnread(0)
                        .build()));

        return toConversationVO(conversation, userId);
    }

    @Override
    @Transactional
    public List<ChatMessageVO> listChatMessages(Long userId, Long conversationId) {
        ChatConversation conversation = requireConversationMember(userId, conversationId);
        if (conversation.getBuyerId().equals(userId)) {
            conversation.setBuyerUnread(0);
        } else {
            conversation.setSellerUnread(0);
        }
        chatConversationRepository.save(conversation);

        return chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(message -> toChatMessageVO(message, conversation, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChatMessageVO sendChatMessage(Long userId, Long conversationId, String content) {
        ChatConversation conversation = requireConversationMember(userId, conversationId);
        String text = content.trim();
        if (text.isBlank()) {
            throw ApiException.badRequest("消息内容不能为空");
        }

        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversationId)
                .senderId(userId)
                .content(text)
                .read(0)
                .createdAt(LocalDateTime.now())
                .build());

        conversation.setLastMessage(text);
        conversation.setUpdatedAt(LocalDateTime.now());
        if (conversation.getBuyerId().equals(userId)) {
            conversation.setSellerUnread(conversation.getSellerUnread() + 1);
        } else {
            conversation.setBuyerUnread(conversation.getBuyerUnread() + 1);
        }
        chatConversationRepository.save(conversation);

        return toChatMessageVO(message, conversation, userId);
    }

    @Override
    public ChatMessageVO getChatMessage(Long userId, Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> ApiException.notFound("消息不存在"));
        ChatConversation conversation = requireConversationMember(userId, message.getConversationId());
        return toChatMessageVO(message, conversation, userId);
    }

    @Override
    public Set<Long> getConversationParticipantIds(Long userId, Long conversationId) {
        ChatConversation conversation = requireConversationMember(userId, conversationId);
        return Set.of(conversation.getBuyerId(), conversation.getSellerId());
    }

    private ChatConversation requireConversationMember(Long userId, Long conversationId) {
        ChatConversation conversation = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound("会话不存在"));
        if (!conversation.getBuyerId().equals(userId) && !conversation.getSellerId().equals(userId)) {
            throw ApiException.badRequest("无权访问该会话");
        }
        return conversation;
    }

    private Long resolveSellerId(Long userId, Product product) {
        if (product.getSellerId() != null) {
            return product.getSellerId();
        }
        return userRepository.findFirstByIdNotOrderByIdAsc(userId)
                .map(User::getId)
                .orElse(userId);
    }

    private ConversationVO toConversationVO(ChatConversation conversation, Long currentUserId) {
        Product product = productRepository.findById(conversation.getProductId()).orElse(null);
        User buyer = userRepository.findById(conversation.getBuyerId()).orElse(null);
        User seller = userRepository.findById(conversation.getSellerId()).orElse(null);
        boolean currentIsBuyer = conversation.getBuyerId().equals(currentUserId);

        return ConversationVO.builder()
                .id(String.valueOf(conversation.getId()))
                .productId(String.valueOf(conversation.getProductId()))
                .productTitle(product != null ? product.getTitle() : "商品已下架")
                .productImage(product != null ? product.getImage() : "")
                .buyerName(displayName(buyer))
                .sellerName(displayName(seller))
                .lastMessage(conversation.getLastMessage() != null ? conversation.getLastMessage() : "")
                .updatedAt(format(conversation.getUpdatedAt()))
                .unread(currentIsBuyer ? conversation.getBuyerUnread() : conversation.getSellerUnread())
                .build();
    }

    private ChatMessageVO toChatMessageVO(ChatMessage message, ChatConversation conversation, Long currentUserId) {
        User sender = userRepository.findById(message.getSenderId()).orElse(null);
        boolean senderIsBuyer = conversation.getBuyerId().equals(message.getSenderId());
        return ChatMessageVO.builder()
                .id(String.valueOf(message.getId()))
                .conversationId(String.valueOf(message.getConversationId()))
                .senderName(displayName(sender))
                .senderRole(senderIsBuyer ? "buyer" : "seller")
                .content(message.getContent())
                .createdAt(format(message.getCreatedAt()))
                .self(message.getSenderId().equals(currentUserId))
                .build();
    }

    private String displayName(User user) {
        if (user == null) return "用户";
        if (user.getNickname() != null && !user.getNickname().isBlank()) return user.getNickname();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername();
        return "用户" + user.getId();
    }

    private String format(LocalDateTime time) {
        return time != null ? time.format(FORMATTER) : "";
    }
}
