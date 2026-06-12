package com.tyut.mall.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "mall_product_sku")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductSku {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(length = 128, nullable = false)
    private String spec;  // "500g" / "曜石黑" etc.

    @Column(precision = 10, scale = 2)
    private BigDecimal price;  // 此 SKU 价格，null 则用 Product.price

    @Column @Builder.Default
    private Integer stock = 0;

    @Column @Builder.Default
    private Integer status = 1;
}
