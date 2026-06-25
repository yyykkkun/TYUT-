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
    private final MessageRepository messageRepository;
    private final BalanceRecordRepository balanceRecordRepository;

    /** 30分钟未支付自动取消 */
    private static final int PAY_TIMEOUT_MINUTES = 30;

    @Override
    public List<OrderVO> list(Long userId, String status, Integer page, Integer pageSize) {
        cancelExpiredOrders(userId);
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
        cancelExpiredOrders(userId);
        Order order = findOwnOrder(userId, orderId);
        // 如果订单已过期未支付，自动取消
        if ("pending_payment".equals(order.getStatus())
                && order.getCreatedAt() != null
                && order.getCreatedAt().plusMinutes(PAY_TIMEOUT_MINUTES).isBefore(LocalDateTime.now())) {
            cancelOrderInternal(order);
        }
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

        // 如果使用了余额支付，记录支付方式
        String paymentMethod = null;
        if (price.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0) {
            paymentMethod = "balance";
        }

        Order order = Order.builder()
                .orderNo(orderNo)
                .userId(userId)
                .status("pending_payment")
                .deliveryMethod(request.getDeliveryMethod() != null ? request.getDeliveryMethod() : "platform")
                .addressSnapshot(addressSnapshot)
                .couponAmount(price.getCouponAmount())
                .pointsUsed(price.getPointsAmount().multiply(BigDecimal.valueOf(100)).intValue())
                .giftCardAmount(price.getGiftCardAmount())
                .balanceAmount(price.getBalanceAmount())
                .freight(price.getFreight())
                .total(price.getTotal())
                .paymentMethod(paymentMethod)
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

        // 扣减积分、礼品卡和余额
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setPoints(user.getPoints() - order.getPointsUsed());
            user.setGiftCard(user.getGiftCard().subtract(order.getGiftCardAmount()));
            user.setBalance(user.getBalance().subtract(order.getBalanceAmount()));
            userRepository.save(user);
            if (order.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0) {
                balanceRecordRepository.save(BalanceRecord.builder()
                        .userId(userId).amount(order.getBalanceAmount().negate()).type("order_pay")
                        .orderNo(order.getOrderNo()).balanceAfter(user.getBalance())
                        .remark("订单支付扣除余额").build());
            }
        }

        // 清空购物车已选项
        cartItemRepository.deleteSelectedByUserId(userId);

        // 发送订单创建通知
        sendNotification(userId, "订单已创建",
                "订单 " + orderNo + " 已创建，共 " + order.getTotal() + " 元，请尽快支付。", "order");

        // 返回数据库 ID（不是 orderNo），与 OrderVO.id 一致，前端可正确跳转
        return String.valueOf(order.getId());
    }

    @Override
    @Transactional
    public void pay(Long userId, Long orderId, String paymentMethod) {
        Order order = findOwnOrder(userId, orderId);
        if (!"pending_payment".equals(order.getStatus())) {
            throw ApiException.badRequest("订单状态不允许付款");
        }

        // 如果支付方式为余额，扣减余额
        if ("balance".equals(paymentMethod)) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                if (user.getBalance().compareTo(order.getTotal()) < 0) {
                    throw ApiException.badRequest("余额不足");
                }
                user.setBalance(user.getBalance().subtract(order.getTotal()));
                userRepository.save(user);
                balanceRecordRepository.save(BalanceRecord.builder()
                        .userId(userId).amount(order.getTotal().negate()).type("order_pay")
                        .orderNo(order.getOrderNo()).balanceAfter(user.getBalance())
                        .remark("余额支付").build());
            }
        }

        order.setStatus("paid");
        order.setPaidAt(LocalDateTime.now());
        order.setDeliveryNo("YT" + String.format("%08d", new Random().nextInt(90_000_000) + 10_000_000));
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "wechat");
        orderRepository.save(order);

        sendNotification(userId, "支付成功",
                "订单 " + order.getOrderNo() + " 已支付，我们将尽快发货。", "order");
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

        // 恢复用户积分、礼品卡和余额
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setPoints(user.getPoints() + order.getPointsUsed());
            user.setGiftCard(user.getGiftCard().add(order.getGiftCardAmount()));
            user.setBalance(user.getBalance().add(order.getBalanceAmount()));
            userRepository.save(user);
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

        // 确认收货后：赠积分 + 加成长值
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            // 消费1元=1积分
            int pointsEarned = order.getTotal().intValue();
            user.setPoints(user.getPoints() + pointsEarned);
            // 成长值
            int growthGain = order.getTotal().intValue();
            user.setGrowth(user.getGrowth() + growthGain);
            user.setLevel(getLevelByGrowth(user.getGrowth()));
            userRepository.save(user);
            // 记积分流水
            balanceRecordRepository.save(BalanceRecord.builder()
                    .userId(userId).amount(BigDecimal.valueOf(pointsEarned)).type("points_earn")
                    .orderNo(order.getOrderNo()).balanceAfter(BigDecimal.valueOf(user.getPoints()))
                    .remark("确认收货赠送积分（1元=1分）").build());
        }

        sendNotification(userId, "收货确认",
                "订单 " + order.getOrderNo() + " 已确认收货，赠送 " + (order.getTotal().intValue()) + " 积分！", "order");
    }

    @Override
    @Transactional
    public void requestRefund(Long userId, Long orderId, String reason) {
        Order order = findOwnOrder(userId, orderId);
        if (!"completed".equals(order.getStatus()) && !"paid".equals(order.getStatus())) {
            throw ApiException.badRequest("只有已完成或已付款的订单可以申请退款");
        }
        if (order.getRefundStatus() != null && !order.getRefundStatus().equals("rejected")) {
            throw ApiException.badRequest("已申请过退款，请勿重复申请");
        }
        order.setRefundStatus("pending");
        order.setRefundReason(reason);
        orderRepository.save(order);
        sendNotification(userId, "退款申请已提交",
                "订单 " + order.getOrderNo() + " 的退款申请已提交，请等待审核。", "order");
    }

    @Override
    @Transactional
    public void processRefund(Long orderId, boolean approved) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("订单不存在"));
        if (!"pending".equals(order.getRefundStatus())) {
            throw ApiException.badRequest("该订单没有待审核的退款申请");
        }

        if (approved) {
            order.setRefundStatus("completed");
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

            // 恢复用户资产
            User user = userRepository.findById(order.getUserId()).orElse(null);
            if (user != null) {
                user.setBalance(user.getBalance().add(order.getTotal()));
                user.setPoints(user.getPoints() + order.getPointsUsed());
                user.setGiftCard(user.getGiftCard().add(order.getGiftCardAmount()));
                user.setBalance(user.getBalance().add(order.getBalanceAmount()));
                userRepository.save(user);
                if (order.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0) {
                    balanceRecordRepository.save(BalanceRecord.builder()
                            .userId(user.getId()).amount(order.getBalanceAmount()).type("order_refund")
                            .orderNo(order.getOrderNo()).balanceAfter(user.getBalance())
                            .remark("退款退回余额").build());
                }
            }

            sendNotification(order.getUserId(), "退款已通过",
                    "订单 " + order.getOrderNo() + " 的退款申请已通过，款项将退回您的账户。", "order");
        } else {
            order.setRefundStatus("rejected");
            sendNotification(order.getUserId(), "退款被拒绝",
                    "订单 " + order.getOrderNo() + " 的退款申请未通过审核。", "order");
        }
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void review(Long userId, Long orderId, String review, Integer rating) {
        Order order = findOwnOrder(userId, orderId);
        if (!"completed".equals(order.getStatus())) {
            throw ApiException.badRequest("只能评价已完成的订单");
        }
        order.setReview(review);
        order.setRating(rating != null ? rating : 5);
        orderRepository.save(order);
    }

    // ========== private helpers ==========

    /** 会员等级换算（成长值 → 等级 → 折扣率） */
    private String getLevelByGrowth(Integer growth) {
        if (growth == null) return "普通会员";
        if (growth >= 10000) return "钻石会员";
        if (growth >= 5000) return "黄金会员";
        if (growth >= 2000) return "白银会员";
        if (growth >= 500) return "青铜会员";
        return "普通会员";
    }

    /** 发送站内信通知 */
    private void sendNotification(Long userId, String title, String body, String type) {
        try {
            Message msg = Message.builder()
                    .userId(userId)
                    .title(title)
                    .body(body)
                    .type(type)
                    .read(0)
                    .build();
            messageRepository.save(msg);
        } catch (Exception ignored) {
            // 通知发送失败不影响主流程
        }
    }

    /** 批量取消当前用户所有过期未支付订单 */
    private void cancelExpiredOrders(Long userId) {
        List<Order> pendingOrders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, "pending_payment", PageRequest.of(0, 100)).getContent();
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(PAY_TIMEOUT_MINUTES);
        for (Order o : pendingOrders) {
            if (o.getCreatedAt() != null && o.getCreatedAt().isBefore(deadline)) {
                cancelOrderInternal(o);
            }
        }
    }

    /** 内部取消订单（不检查状态） */
    private void cancelOrderInternal(Order order) {
        order.setStatus("cancelled");
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem oi : items) {
            Product p = productRepository.findById(oi.getProductId()).orElse(null);
            if (p != null) {
                p.setStock(p.getStock() + oi.getQuantity());
                p.setSales(Math.max(0, p.getSales() - oi.getQuantity()));
                productRepository.save(p);
            }
        }
        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            user.setPoints(user.getPoints() + order.getPointsUsed());
            user.setGiftCard(user.getGiftCard().add(order.getGiftCardAmount()));
            user.setBalance(user.getBalance().add(order.getBalanceAmount()));
            userRepository.save(user);
            if (order.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0) {
                balanceRecordRepository.save(BalanceRecord.builder()
                        .userId(user.getId()).amount(order.getBalanceAmount()).type("order_cancel")
                        .orderNo(order.getOrderNo()).balanceAfter(user.getBalance())
                        .remark("订单取消退回余额").build());
            }
        }
        orderRepository.save(order);
    }

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
        BigDecimal balanceAmount = BigDecimal.ZERO;

        User user = userRepository.findById(userId).orElse(null);

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
            if (user != null) {
                long maxPointsDeduct = Math.min(user.getPoints() / 100, 20);
                BigDecimal maxBySubtotal = subtotal.subtract(couponAmount);
                long actual = Math.min(maxPointsDeduct, maxBySubtotal.longValue());
                pointsAmount = BigDecimal.valueOf(Math.max(0, actual));
            }
        }

        // 礼品卡
        if (request.getUseGiftCard() != null && request.getUseGiftCard()) {
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

        // 会员等级折扣
        BigDecimal memberDiscount = BigDecimal.ZERO;
        if (user != null) {
            String level = user.getLevel() != null ? user.getLevel() : getLevelByGrowth(user.getGrowth());
            memberDiscount = switch (level) {
                case "钻石会员" -> subtotal.multiply(BigDecimal.valueOf(0.05));  // 95折
                case "黄金会员" -> subtotal.multiply(BigDecimal.valueOf(0.03));  // 97折
                case "白银会员" -> subtotal.multiply(BigDecimal.valueOf(0.02));  // 98折
                default -> BigDecimal.ZERO;
            };
        }

        // 计算扣除优惠券/积分/礼品卡后的待付金额
        BigDecimal beforeBalance = subtotal.subtract(couponAmount).subtract(pointsAmount)
                .subtract(giftCardAmount).subtract(memberDiscount).add(freight).max(BigDecimal.ZERO);

        // 余额支付：用余额抵扣剩余的待付金额
        if (request.getUseBalance() != null && request.getUseBalance() && user != null) {
            balanceAmount = user.getBalance().min(beforeBalance).max(BigDecimal.ZERO);
        }

        BigDecimal total = beforeBalance.subtract(balanceAmount).max(BigDecimal.ZERO);

        return OrderPreviewVO.builder()
                .subtotal(subtotal)
                .couponAmount(couponAmount)
                .pointsAmount(pointsAmount)
                .giftCardAmount(giftCardAmount)
                .balanceAmount(balanceAmount)
                .memberDiscount(memberDiscount)
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
                .balanceAmount(order.getBalanceAmount())
                .freight(order.getFreight())
                .total(order.getTotal())
                .paidAt(order.getPaidAt() != null ? order.getPaidAt().toString() : null)
                .deliveryNo(order.getDeliveryNo())
                .review(order.getReview())
                .rating(order.getRating())
                .paymentMethod(order.getPaymentMethod())
                .refundStatus(order.getRefundStatus())
                .refundReason(order.getRefundReason())
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
