package com.tyut.mall.controller;

import com.alibaba.fastjson2.JSON;
import com.tyut.mall.common.ApiResponse;
import com.tyut.mall.dto.response.ProductVO;
import com.tyut.mall.entity.Product;
import com.tyut.mall.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final ProductRepository productRepository;

    @GetMapping("/seckill")
    public ApiResponse<?> seckill() {
        List<Product> products = productRepository.findByPromotionAndStatus("seckill", 1);
        return ApiResponse.ok(products.stream().map(this::toVO).collect(Collectors.toList()));
    }

    @GetMapping("/group-buy")
    public ApiResponse<?> groupBuy() {
        List<Product> products = productRepository.findByPromotionAndStatus("group-buy", 1);
        return ApiResponse.ok(products.stream().map(this::toVO).collect(Collectors.toList()));
    }

    private ProductVO toVO(Product p) {
        return ProductVO.builder()
                .id(String.valueOf(p.getId()))
                .title(p.getTitle())
                .subtitle(p.getSubtitle())
                .category(p.getCategoryId() != null ? String.valueOf(p.getCategoryId()) : null)
                .brand(p.getBrand())
                .origin(p.getOrigin())
                .city(p.getCity())
                .tags(p.getTags() != null ? JSON.parseArray(p.getTags(), String.class) : List.of())
                .price(p.getPrice())
                .originalPrice(p.getOriginalPrice())
                .stock(p.getStock())
                .sales(p.getSales())
                .popularity(p.getPopularity())
                .rating(p.getRating())
                .image(p.getImage())
                .specs(p.getSpecs() != null ? JSON.parseArray(p.getSpecs(), String.class) : List.of())
                .promotion(p.getPromotion())
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toString() : null)
                .build();
    }
}
