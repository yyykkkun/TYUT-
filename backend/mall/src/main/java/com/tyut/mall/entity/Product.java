package com.tyut.mall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mall_product")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(length = 255)
    private String subtitle;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(length = 64)
    private String brand;

    @Column(length = 64)
    private String origin;

    @Column(length = 64)
    private String city;

    @Column(length = 500)
    private String tags;   // JSON 数组字符串

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Builder.Default
    private Integer stock = 0;

    @Builder.Default
    private Integer sales = 0;

    @Builder.Default
    private Integer popularity = 0;

    @Column(precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal rating = BigDecimal.valueOf(5.0);

    @Column(length = 500)
    private String image;

    @Column(length = 500)
    private String specs;  // JSON 数组字符串

    @Column(length = 32)
    private String listingType;  // idle / urgent / campus

    @Column(name = "condition_desc", length = 64)
    private String condition;    // 全新 / 九成新 / 有使用痕迹等

    @Column(length = 32)
    private String promotion;

    @Builder.Default
    private Integer status = 1;  // 0下架 1上架

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
