package com.tyut.mall.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {
    private String id;
    private String conversationId;
    private String senderName;
    private String senderRole;
    private String content;
    private String createdAt;
    private Boolean self;
}
