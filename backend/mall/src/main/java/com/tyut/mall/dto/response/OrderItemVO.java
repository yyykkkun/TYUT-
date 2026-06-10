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
public class OrderItemVO {
    private String productId;
    private String title;
    private String image;
    private String spec;
    private BigDecimal price;
    private Integer quantity;
}
