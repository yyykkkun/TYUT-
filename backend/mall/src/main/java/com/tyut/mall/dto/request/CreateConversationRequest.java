package com.tyut.mall.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConversationRequest {

    @NotNull(message = "商品不能为空")
    private Long productId;
}
