package com.tyut.mall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mall_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", length = 32, unique = true, nullable = false)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 32, nullable = false)
    private String status;  // pending_payment / paid / shipping / completed / cancelled

    @Column(name = "delivery_method", length = 32)
    private String deliveryMethod;

    @Column(name = "address_snapshot", columnDefinition = "TEXT")
    private String addressSnapshot;  // JSON 快照

    @Column(name = "coupon_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal couponAmount = BigDecimal.ZERO;

    @Column(name = "points_used")
    @Builder.Default
    private Integer pointsUsed = 0;

    @Column(name = "gift_card_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal giftCardAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal freight = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal total;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "delivery_no", length = 64)
    private String deliveryNo;

    @Column(columnDefinition = "TEXT")
    private String review;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
