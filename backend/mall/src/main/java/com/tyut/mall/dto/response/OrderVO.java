package com.tyut.mall.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {
    private String id;
    private String status;
    private String createdAt;
    private List<OrderItemVO> items;
    private AddressVO address;
    private String deliveryMethod;
    private BigDecimal couponAmount;
    private Integer pointsUsed;
    private BigDecimal giftCardAmount;
    private BigDecimal balanceAmount;
    private BigDecimal freight;
    private BigDecimal total;
    private String paidAt;
    private String deliveryNo;
    private String review;
    private Integer rating;
    private String paymentMethod;
    private String refundStatus;
    private String refundReason;
}
