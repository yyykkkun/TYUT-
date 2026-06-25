package com.tyut.mall.controller;

import com.tyut.mall.common.ApiResponse;
import com.tyut.mall.common.UserContext;
import com.tyut.mall.dto.request.PublishProductRequest;
import com.tyut.mall.dto.response.ReviewVO;
import com.tyut.mall.entity.Order;
import com.tyut.mall.entity.OrderItem;
import com.tyut.mall.entity.User;
import com.tyut.mall.repository.OrderItemRepository;
import com.tyut.mall.repository.OrderRepository;
import com.tyut.mall.repository.ProductSkuRepository;
import com.tyut.mall.repository.UserRepository;
import com.tyut.mall.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductSkuRepository skuRepository;

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

    @GetMapping("/my")
    public ApiResponse<?> mine() {
        Long userId = getUserId();
        return ApiResponse.ok(productService.mine(userId));
    }

    @PostMapping
    public ApiResponse<?> publish(@Valid @RequestBody PublishProductRequest request) {
        Long userId = getUserId();
        return ApiResponse.ok(productService.publish(userId, request));
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

    /** 获取商品 SKU 列表 */
    @GetMapping("/{id}/skus")
    public ApiResponse<?> skus(@PathVariable Long id) {
        return ApiResponse.ok(skuRepository.findByProductIdAndStatus(id, 1));
    }

    /** 获取商品的所有评价 */
    @GetMapping("/{id}/reviews")
    public ApiResponse<?> reviews(@PathVariable Long id) {
        List<OrderItem> items = orderItemRepository.findByProductId(id);
        List<ReviewVO> reviews = new ArrayList<>();
        for (OrderItem item : items) {
            Order order = orderRepository.findById(item.getOrderId()).orElse(null);
            if (order == null || order.getReview() == null || order.getReview().isBlank()) continue;
            User user = userRepository.findById(order.getUserId()).orElse(null);
            String content = order.getReview();
            List<String> images = new ArrayList<>();
            // 解析评价中的图片
            int imgIdx = content.indexOf("[图片]");
            if (imgIdx >= 0) {
                String imgPart = content.substring(imgIdx + 4);
                content = content.substring(0, imgIdx).trim();
                images = List.of(imgPart.split(","));
            }
            reviews.add(ReviewVO.builder()
                    .id(String.valueOf(order.getId()))
                    .orderId(order.getOrderNo())
                    .userName(user != null ? user.getNickname() : "匿名用户")
                    .userAvatar(user != null ? user.getAvatar() : null)
                    .rating(order.getRating() != null ? order.getRating() : 5)
                    .content(content)
                    .images(images)
                    .createdAt(order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null)
                    .build());
        }
        return ApiResponse.ok(reviews);
    }

    private Long getUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        return userId;
    }
}
