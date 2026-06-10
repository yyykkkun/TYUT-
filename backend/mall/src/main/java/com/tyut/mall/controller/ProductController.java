package com.tyut.mall.controller;

import com.tyut.mall.common.ApiResponse;
import com.tyut.mall.common.UserContext;
import com.tyut.mall.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) String brand,
                                @RequestParam(required = false) String stock,
                                @RequestParam(required = false) String city,
                                @RequestParam(required = false) Double minPrice,
                                @RequestParam(required = false) Double maxPrice,
                                @RequestParam(required = false, defaultValue = "composite") String sort,
                                @RequestParam(required = false, defaultValue = "1") Integer page,
                                @RequestParam(required = false, defaultValue = "6") Integer pageSize) {
        return ApiResponse.ok(productService.list(keyword, category, brand, stock, city,
                minPrice, maxPrice, sort, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return ApiResponse.ok(productService.detail(id, userId));
    }

    @GetMapping("/hot")
    public ApiResponse<?> hot() {
        return ApiResponse.ok(productService.hot());
    }

    @GetMapping("/latest")
    public ApiResponse<?> latest() {
        return ApiResponse.ok(productService.latest());
    }

    @GetMapping("/special")
    public ApiResponse<?> special() {
        return ApiResponse.ok(productService.special());
    }

    @GetMapping("/recommended")
    public ApiResponse<?> recommended() {
        Long userId = UserContext.getUserId();
        return ApiResponse.ok(productService.recommended(userId));
    }
}
