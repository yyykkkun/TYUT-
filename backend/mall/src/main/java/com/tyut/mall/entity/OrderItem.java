package com.tyut.mall.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "mall_order_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id")
    private Long productId;

    @Column(length = 255)
    private String title;

    @Column(length = 500)
    private String image;

    @Column(length = 64)
    private String spec;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private Integer quantity;
}
