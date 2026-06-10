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
public class MemberProfileVO {
    private BigDecimal balance;
    private Integer points;
    private BigDecimal giftCard;
    private Integer growth;
    private String level;
    private Integer couponCount;
    private Integer unreadCount;
}
