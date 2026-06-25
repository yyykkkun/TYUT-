package com.tyut.mall.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVO {
    private String id;
    private String productId;
    private String productTitle;
    private String productImage;
    private String buyerName;
    private String sellerName;
    private String lastMessage;
    private String updatedAt;
    private Integer unread;
}
