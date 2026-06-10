package com.tyut.mall.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPageVO {
    private List<ProductVO> products;
    private Integer total;
    private Integer page;
    private Integer pageSize;
    private Integer totalPages;
}
