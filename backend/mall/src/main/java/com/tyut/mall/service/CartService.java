package com.tyut.mall.service;

import com.tyut.mall.dto.request.AddCartRequest;
import com.tyut.mall.dto.response.CartItemVO;

import java.util.List;

public interface CartService {
    List<CartItemVO> list(Long userId);
    void add(Long userId, AddCartRequest request);
    void updateQuantity(Long userId, Long cartItemId, Integer quantity);
    void toggleSelect(Long userId, Long cartItemId);
    void remove(Long userId, Long cartItemId);
    void clearSelected(Long userId);
}
