package com.tyut.mall.service.impl;

import com.alibaba.fastjson2.JSON;
import com.tyut.mall.common.ApiException;
import com.tyut.mall.dto.request.OrderPreviewRequest;
import com.tyut.mall.dto.response.*;
import com.tyut.mall.entity.*;
import com.tyut.mall.repository.*;
import com.tyut.mall.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;

    @Override
    public List<OrderVO> list(Long userId, String status, Integer page, Integer pageSize) {
        int pageNum = page != null ? page : 1;
        int size = pageSize != null ? pageSize : 10;
        Page<Order> orderPage;
        if (status == null || "all".equals(status)) {
            orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(pageNum - 1, size));
        } else {
            orderPage = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, PageRequest.of(pageNum - 1, size));
        }
        return orderPage.getContent().stream().map(this::toOrderVO).collect(Collectors.toList());
    }

    @Override
    public OrderVO detail(Long userId, Long orderId) {
        Order order = findOwnOrder(userId, orderId);
        return toOrderVO(order);
    }

    @Override
    public OrderPreviewVO preview(Long userId, OrderPreviewRequest request) {
        // 获取已选中的购物车项
        List<CartItem> selectedItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().filter(c -> c.getSelected() == 1).toList();

        if (selectedItems.isEmpty()) {
            throw ApiException.badRequest("没有选中的商品");
        }

        BigDecimal subtotal = selectedItems.stream()
                .map(c -> {
                    Product p = productRepository.findById(c.getProductId())
                            .orElseThrow(() -> ApiException.notFound("商品不存在"));
                    return p.getPrice().multiply(BigDecimal.valueOf(c.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return calculatePrice(userId, request, subtotal);
    }

    @Override
    @Transactional
    public String create(Long userId, OrderPreviewRequest request) {
        // 获取已选中的购物车项
        List<CartItem> selectedItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().filter(c -> c.getSelected() == 1).toList();

        if (selectedItems.isEmpty()) {
            throw ApiException.badRequest("没有选中的商品");
        }

        // 获取默认地址
        Address defaultAddress = addressRepository.findByUserIdAndIsDefault(userId, 1)
                .orElseThrow(() -> ApiException.badRequest("请先设置收货地址"));

        BigDecimal subtotal = selectedItems.stream()
                .map(c -> {
                    Product p = productRepository.findById(c.getProductId())
                            .orElseThrow(() -> ApiException.notFound("商品不存在"));
                    if (p.getStock() < c.getQuantity()) {
                        throw ApiException.badRequest(p.getTitle() + " 库存不足");
                    }
                    return p.getPrice().multiply(BigDecimal.valueOf(c.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算价格
        OrderPreviewVO price = calculatePrice(userId, request, subtotal);

        // 生成订单号
        String orderNo = "SO" + System.currentTimeMillis();

        // 保存地址快照
        String addressSnapshot = JSON.toJSONString(toAddressVO(defaultAddress));

        Order order = Order.builder()
                .orderNo(orderNo)
                .userId(userId)
                .status("pending_payment")
                .deliveryMethod(request.getDeliveryMethod() != null ? request.getDeliveryMethod() : "platform")
                .addressSnapshot(addressSnapshot)
                .couponAmount(price.getCouponAmount())
                .pointsUsed(price.getPointsAmount().multiply(BigDecimal.valueOf(100)).intValue())
                .giftCardAmount(price.getGiftCardAmount())
                .freight(price.getFreight())
                .total(price.getTotal())
                .build();
        order = orderRepository.save(order);

        // 保存订单项
        for (CartItem ci : selectedItems) {
            Product p = productRepository.findById(ci.getProductId()).orElse(null);
            OrderItem oi = OrderItem.builder()
                    .orderId(order.getId())
                    .productId(ci.getProductId())
                    .title(p != null ? p.getTitle() : "")
                    .image(p != null ? p.getImage() : "")
                    .spec(ci.getSpec())
                    .price(p != null ? p.getPrice() : BigDecimal.ZERO)
                    .quantity(ci.getQuantity())
                    .build();
            orderItemRepository.save(oi);

            // 扣减库存
            if (p != null) {
                p.setStock(p.getStock() - ci.getQuantity());
                p.setSales(p.getSales() + ci.getQuantity());
                productRepository.save(p);
            }
        }

        // 扣减优惠券
        if (request.getCouponId() != null) {
            var userCoupon = userCouponRepository.findByIdAndUserId(request.getCouponId(), userId);
            if (userCoupon.isPresent() && userCoupon.get().getUsed() == 0) {
                UserCoupon uc = userCoupon.get();
                uc.setUsed(1);
                uc.setUsedAt(LocalDateTime.now());
                userCouponRepository.save(uc);
            }
        }

        // 扣减积分和礼品卡
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setPoints(user.getPoints() - order.getPointsUsed());
            user.setGiftCard(user.getGiftCard().subtract(order.getGiftCardAmount()));
            userRepository.save(user);
        }

        // 清空购物车已选项
        cartItemRepository.deleteSelectedByUserId(userId);

        return orderNo;
    }

    @Override
    @Transactional
    public void pay(Long userId, Long orderId) {
        Order order = findOwnOrder(userId, orderId);
        if (!"pending_payment".equals(order.getStatus())) {
            throw ApiException.badRequest("订单状态不允许付款");
        }
        order.setStatus("paid");
        order.setPaidAt(LocalDateTime.now());
        order.setDeliveryNo("YT" + String.format("%08d", new Random().nextInt(90_000_000) + 10_000_000));
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long orderId) {
        Order order = findOwnOrder(userId, orderId);
        if (!"pending_payment".equals(order.getStatus())) {
            throw ApiException.badRequest("只有待付款订单可以取消");
        }
        order.setStatus("cancelled");

        // 恢复库存
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem oi : items) {
            Product p = productRepository.findById(oi.getProductId()).orElse(null);
            if (p != null) {
                p.setStock(p.getStock() + oi.getQuantity());
                p.setSales(Math.max(0, p.getSales() - oi.getQuantity()));
                productRepository.save(p);
            }
        }
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void confirm(Long userId, Long orderId) {
        Order order = findOwnOrder(userId, orderId);
        if (!"paid".equals(order.getStatus()) && !"shipping".equals(order.getStatus())) {
            throw ApiException.badRequest("订单状态不允许确认收货");
        }
        order.setStatus("completed");
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void review(Long userId, Long orderId, String review) {
        Order order = findOwnOrder(userId, orderId);
        if (!"completed".equals(order.getStatus())) {
            throw ApiException.badRequest("只能评价已完成的订单");
        }
        order.setReview(review);
        orderRepository.save(order);
    }

    // ========== private helpers ==========

    private Order findOwnOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("订单不存在"));
        if (!order.getUserId().equals(userId)) {
            throw ApiException.badRequest("无权操作");
        }
        return order;
    }

    private OrderPreviewVO calculatePrice(Long userId, OrderPreviewRequest request, BigDecimal subtotal) {
        String deliveryMethod = request.getDeliveryMethod() != null ? request.getDeliveryMethod() : "platform";
        BigDecimal couponAmount = BigDecimal.ZERO;
        BigDecimal pointsAmount = BigDecimal.ZERO;
        BigDecimal giftCardAmount = BigDecimal.ZERO;

        // 优惠券
        if (request.getCouponId() != null) {
            var userCoupon = userCouponRepository.findByIdAndUserId(request.getCouponId(), userId);
            if (userCoupon.isPresent() && userCoupon.get().getUsed() == 0) {
                Coupon coupon = couponRepository.findById(userCoupon.get().getCouponId()).orElse(null);
                if (coupon != null && subtotal.compareTo(coupon.getThreshold()) >= 0) {
                    couponAmount = coupon.getAmount();
                }
            }
        }

        // 积分抵扣
        if (request.getUsePoints() != null && request.getUsePoints()) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                long maxPointsDeduct = Math.min(user.getPoints() / 100, 20);
                BigDecimal maxBySubtotal = subtotal.subtract(couponAmount);
                long actual = Math.min(maxPointsDeduct, maxBySubtotal.longValue());
                pointsAmount = BigDecimal.valueOf(Math.max(0, actual));
            }
        }

        // 礼品卡
        if (request.getUseGiftCard() != null && request.getUseGiftCard()) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                BigDecimal remaining = subtotal.subtract(couponAmount).subtract(pointsAmount);
                BigDecimal maxByCard = user.getGiftCard().min(BigDecimal.valueOf(50));
                giftCardAmount = maxByCard.min(remaining).max(BigDecimal.ZERO);
            }
        }

        // 运费: 满199或自提免运费
        BigDecimal freight = BigDecimal.ZERO;
        if (!"pickup".equals(deliveryMethod) && subtotal.compareTo(BigDecimal.valueOf(199)) < 0) {
            freight = BigDecimal.valueOf(12);
        }

        BigDecimal total = subtotal.subtract(couponAmount).subtract(pointsAmount)
                .subtract(giftCardAmount).add(freight).max(BigDecimal.ZERO);

        return OrderPreviewVO.builder()
                .subtotal(subtotal)
                .couponAmount(couponAmount)
                .pointsAmount(pointsAmount)
                .giftCardAmount(giftCardAmount)
                .freight(freight)
                .total(total)
                .build();
    }

    private OrderVO toOrderVO(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemVO> itemVOs = items.stream()
                .map(oi -> OrderItemVO.builder()
                        .productId(String.valueOf(oi.getProductId()))
                        .title(oi.getTitle())
                        .image(oi.getImage())
                        .spec(oi.getSpec())
                        .price(oi.getPrice())
                        .quantity(oi.getQuantity())
                        .build())
                .collect(Collectors.toList());

        AddressVO addressVO = null;
        if (order.getAddressSnapshot() != null) {
            try {
                addressVO = JSON.parseObject(order.getAddressSnapshot(), AddressVO.class);
            } catch (Exception ignored) {}
        }

        return OrderVO.builder()
                .id(String.valueOf(order.getId()))
                .status(order.getStatus())
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
                .items(itemVOs)
                .address(addressVO)
                .deliveryMethod(order.getDeliveryMethod())
                .couponAmount(order.getCouponAmount())
                .pointsUsed(order.getPointsUsed())
                .giftCardAmount(order.getGiftCardAmount())
                .freight(order.getFreight())
                .total(order.getTotal())
                .paidAt(order.getPaidAt() != null ? order.getPaidAt().toString() : null)
                .deliveryNo(order.getDeliveryNo())
                .review(order.getReview())
                .build();
    }

    private AddressVO toAddressVO(Address a) {
        return AddressVO.builder()
                .id(String.valueOf(a.getId()))
                .name(a.getName())
                .phone(a.getPhone())
                .province(a.getProvince())
                .city(a.getCity())
                .district(a.getDistrict())
                .detail(a.getDetail())
                .isDefault(a.getIsDefault() == 1)
                .build();
    }
}
