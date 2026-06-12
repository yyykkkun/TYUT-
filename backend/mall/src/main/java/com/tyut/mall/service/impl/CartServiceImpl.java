package com.tyut.mall.service.impl;

import com.alibaba.fastjson2.JSON;
import com.tyut.mall.common.ApiException;
import com.tyut.mall.dto.request.AddCartRequest;
import com.tyut.mall.dto.response.CartItemVO;
import com.tyut.mall.dto.response.ProductVO;
import com.tyut.mall.entity.CartItem;
import com.tyut.mall.entity.Product;
import com.tyut.mall.repository.CartItemRepository;
import com.tyut.mall.repository.ProductRepository;
import com.tyut.mall.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Override
    public List<CartItemVO> list(Long userId) {
        return cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toVO)
                .filter(vo -> vo.getProduct() != null)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void add(Long userId, AddCartRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ApiException.notFound("商品不存在"));
        if (product.getStatus() == 0 || product.getStock() <= 0) {
            throw ApiException.badRequest("商品已下架或库存不足");
        }

        // 已有相同商品+规格则增加数量
        var existing = cartItemRepository.findByUserIdAndProductIdAndSpec(
                userId, request.getProductId(), request.getSpec());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem item = CartItem.builder()
                    .userId(userId)
                    .productId(request.getProductId())
                    .spec(request.getSpec())
                    .quantity(request.getQuantity())
                    .selected(1)
                    .build();
            cartItemRepository.save(item);
        }
    }

    @Override
    @Transactional
    public void updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        CartItem item = findOwnCartItem(userId, cartItemId);
        item.setQuantity(Math.max(1, quantity));
        cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public void toggleSelect(Long userId, Long cartItemId) {
        CartItem item = findOwnCartItem(userId, cartItemId);
        item.setSelected(item.getSelected() == 1 ? 0 : 1);
        cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public void remove(Long userId, Long cartItemId) {
        CartItem item = findOwnCartItem(userId, cartItemId);
        cartItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void clearSelected(Long userId) {
        cartItemRepository.deleteSelectedByUserId(userId);
    }

    private CartItem findOwnCartItem(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> ApiException.notFound("购物车项不存在"));
        if (!item.getUserId().equals(userId)) {
            throw ApiException.badRequest("无权操作");
        }
        return item;
    }

    private CartItemVO toVO(CartItem item) {
        Product product = productRepository.findById(item.getProductId()).orElse(null);
        ProductVO productVO = null;
        if (product != null) {
            productVO = ProductVO.builder()
                    .id(String.valueOf(product.getId()))
                    .title(product.getTitle())
                    .subtitle(product.getSubtitle())
                    .brand(product.getBrand())
                    .city(product.getCity())
                    .price(product.getPrice())
                    .originalPrice(product.getOriginalPrice())
                    .stock(product.getStock())
                    .sales(product.getSales())
                    .popularity(product.getPopularity())
                    .rating(product.getRating())
                    .image(product.getImage())
                    .tags(JSON.parseArray(product.getTags(), String.class))
                    .specs(JSON.parseArray(product.getSpecs(), String.class))
                    .promotion(product.getPromotion())
                    .createdAt(product.getCreatedAt() != null ? product.getCreatedAt().toString() : null)
                    .build();
        }

        return CartItemVO.builder()
                .id(String.valueOf(item.getId()))
                .productId(String.valueOf(item.getProductId()))
                .spec(item.getSpec())
                .quantity(item.getQuantity())
                .selected(item.getSelected() == 1)
                .product(productVO)
                .build();
    }
}
