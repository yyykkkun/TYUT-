package com.tyut.mall.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddCartRequest {
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotBlank(message = "规格不能为空")
    private String spec;

    @Min(1)
    private Integer quantity = 1;
}
