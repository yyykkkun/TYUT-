package com.tyut.mall.dto.request;

import lombok.Data;

@Data
public class PaymentRequest {
    private String paymentMethod;  // wechat / alipay / card / balance
}
