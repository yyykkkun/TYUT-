package com.tyut.mall.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemVO {
    private String id;
    private String productId;
    private String spec;
    private Integer quantity;
    private Boolean selected;
    private ProductVO product;
}
