package com.tyut.mall.service;

import com.tyut.mall.dto.request.OrderPreviewRequest;
import com.tyut.mall.dto.response.OrderPreviewVO;
import com.tyut.mall.dto.response.OrderVO;

import java.util.List;

public interface OrderService {
    List<OrderVO> list(Long userId, String status, Integer page, Integer pageSize);
    OrderVO detail(Long userId, Long orderId);
    OrderPreviewVO preview(Long userId, OrderPreviewRequest request);
    String create(Long userId, OrderPreviewRequest request);
    void pay(Long userId, Long orderId, String paymentMethod);
    void cancel(Long userId, Long orderId);
    void confirm(Long userId, Long orderId);
    void review(Long userId, Long orderId, String review, Integer rating);
    void requestRefund(Long userId, Long orderId, String reason);
    void processRefund(Long orderId, boolean approved);
}
