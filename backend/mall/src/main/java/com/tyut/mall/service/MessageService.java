package com.tyut.mall.service;

import com.tyut.mall.dto.response.MessageVO;

import java.util.List;

public interface MessageService {
    List<MessageVO> list(Long userId);
    void markRead(Long userId, Long messageId);
}
