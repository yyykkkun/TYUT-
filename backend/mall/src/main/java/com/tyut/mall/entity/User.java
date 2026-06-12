package com.tyut.mall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mall_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, unique = true)
    private String username;

    @Column(length = 128)
    private String password;

    @Column(length = 20)
    private String phone;

    @Column(length = 64)
    private String nickname;

    @Column(length = 255)
    private String avatar;

    @Column(length = 32)
    @Builder.Default
    private String level = "普通会员";

    @Column(length = 16)
    @Builder.Default
    private String role = "user";  // user / admin

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Builder.Default
    private Integer points = 0;

    @Column(name = "gift_card", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal giftCard = BigDecimal.ZERO;

    @Builder.Default
    private Integer growth = 0;

    @Builder.Default
    private Integer status = 1;  // 0禁用 1正常

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
