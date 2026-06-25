package com.tyut.mall.service;

import com.tyut.mall.dto.request.PublishProductRequest;
import com.tyut.mall.dto.response.ProductPageVO;
import com.tyut.mall.dto.response.ProductVO;

import java.util.List;

public interface ProductService {
    ProductPageVO list(String keyword, String category, String brand, String stock,
                       String city, Double minPrice, Double maxPrice,
                       String sort, Integer page, Integer pageSize);
    ProductVO detail(Long id, Long userId);
    List<ProductVO> hot();
    List<ProductVO> latest();
    List<ProductVO> special();
    List<ProductVO> recommended(Long userId);
    List<ProductVO> mine(Long userId);
    ProductVO publish(Long userId, PublishProductRequest request);
}
