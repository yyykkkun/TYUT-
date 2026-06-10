package com.tyut.mall.service.impl;

import com.tyut.mall.common.ApiException;
import com.tyut.mall.dto.response.MessageVO;
import com.tyut.mall.repository.MessageRepository;
import com.tyut.mall.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;

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
}
