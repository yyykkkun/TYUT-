package com.tyut.mall.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO {
    private String id;
    private String title;
    private String subtitle;
    private String category;
    private String sellerId;
    private String brand;
    private String origin;
    private String city;
    private List<String> tags;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer sales;
    private Integer popularity;
    private BigDecimal rating;
    private String image;
    private List<String> specs;
    private String createdAt;
    private String promotion;
    private String listingType;
    private String sellerName;
    private BigDecimal sellerRating;
    private String condition;
}
