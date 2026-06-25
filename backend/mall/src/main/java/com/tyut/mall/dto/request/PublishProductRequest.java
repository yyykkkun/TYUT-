package com.tyut.mall.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PublishProductRequest {
    @NotBlank(message = "商品标题不能为空")
    private String title;

    private String subtitle;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    @Min(value = 1, message = "库存至少为1")
    private Integer stock = 1;

    @NotBlank(message = "商品图片不能为空")
    private String image;

    private String city;

    private String condition;

    private String listingType = "idle";

    private List<String> specs;

    private List<String> tags;
}
