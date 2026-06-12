package com.tyut.mall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mall_group_buy")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupBuy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;  // 团长 userId

    @Column(name = "group_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal groupPrice;

    @Column(name = "required_count")
    @Builder.Default
    private Integer requiredCount = 3;  // 成团人数

    @Column(name = "current_count")
    @Builder.Default
    private Integer currentCount = 1;  // 当前参团人数

    @Column(length = 32)
    @Builder.Default
    private String status = "active";  // active / completed / cancelled / expired

    @Column(name = "member_ids", columnDefinition = "TEXT")
    private String memberIds;  // JSON array of userIds

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;  // 团有效期限（24小时）

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
