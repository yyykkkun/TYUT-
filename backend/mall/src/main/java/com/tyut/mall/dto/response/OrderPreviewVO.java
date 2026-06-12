package com.tyut.mall.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPreviewVO {
    private BigDecimal subtotal;
    private BigDecimal couponAmount;
    private BigDecimal pointsAmount;
    private BigDecimal giftCardAmount;
    private BigDecimal balanceAmount;
    private BigDecimal memberDiscount;
    private BigDecimal freight;
    private BigDecimal total;
}
