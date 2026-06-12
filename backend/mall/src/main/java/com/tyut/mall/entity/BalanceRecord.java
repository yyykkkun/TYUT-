package com.tyut.mall.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "mall_balance_record")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BalanceRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;  // 正=收入，负=支出

    @Column(length = 32, nullable = false)
    private String type;  // recharge / order_pay / order_refund / order_cancel

    @Column(name = "order_no", length = 64)
    private String orderNo;

    @Column(name = "balance_after", precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    @Column(length = 128)
    private String remark;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
