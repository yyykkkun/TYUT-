package com.tyut.mall.dto.request;

import lombok.Data;

@Data
public class OrderPreviewRequest {
    private Long couponId;
    private Boolean usePoints = true;
    private Boolean useGiftCard = false;
    private Boolean useBalance = false;
    private String deliveryMethod = "platform";  // platform / third-party / pickup
}
