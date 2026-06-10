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
public class CouponVO {
    private String id;
    private String title;
    private BigDecimal threshold;
    private BigDecimal amount;
    private String expiresAt;
    private Boolean used;
}
