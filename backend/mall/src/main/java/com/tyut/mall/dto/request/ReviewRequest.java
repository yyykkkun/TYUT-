package com.tyut.mall.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotBlank(message = "评价内容不能为空")
    private String review;
    private Integer rating = 5;
}
