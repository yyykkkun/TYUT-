package com.tyut.mall.controller;

import com.alibaba.fastjson2.JSON;
import com.tyut.mall.common.ApiResponse;
import com.tyut.mall.common.UserContext;
import com.tyut.mall.entity.*;
import com.tyut.mall.repository.*;
import com.tyut.mall.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/group-buy")
@RequiredArgsConstructor
public class GroupBuyController {

    private final GroupBuyRepository groupBuyRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /** 商品的所有活跃拼团 */
    @GetMapping("/groups")
    public ApiResponse<?> groups(@RequestParam Long productId) {
        List<GroupBuy> list = groupBuyRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId, "active");
        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupBuy g : list) {
            Map<String, Object> m = toMap(g);
            result.add(m);
        }
        return ApiResponse.ok(result);
    }

    /** 开团 */
    @PostMapping("/create")
    @Transactional
    public ApiResponse<?> create(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        if (userId == null) return ApiResponse.fail(401, "请先登录");

        Long productId = Long.valueOf(body.get("productId").toString());
        Product p = productRepository.findById(productId).orElse(null);
        if (p == null) return ApiResponse.fail(404, "商品不存在");

        BigDecimal groupPrice = new BigDecimal(body.getOrDefault("groupPrice", p.getPrice().multiply(BigDecimal.valueOf(0.8))).toString());
        int required = Integer.parseInt(body.getOrDefault("requiredCount", "3").toString());

        GroupBuy gb = GroupBuy.builder()
                .productId(productId)
                .leaderId(userId)
                .groupPrice(groupPrice)
                .requiredCount(required)
                .currentCount(1)
                .status("active")
                .memberIds(JSON.toJSONString(List.of(userId)))
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        gb = groupBuyRepository.save(gb);

        return ApiResponse.ok(toMap(gb));
    }

    /** 参团 */
    @PostMapping("/{id}/join")
    @Transactional
    public ApiResponse<?> join(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        if (userId == null) return ApiResponse.fail(401, "请先登录");

        GroupBuy gb = groupBuyRepository.findById(id).orElse(null);
        if (gb == null) return ApiResponse.fail(404, "拼团不存在");
        if (!"active".equals(gb.getStatus())) return ApiResponse.fail(400, "该团已结束");
        if (gb.getExpiresAt() != null && gb.getExpiresAt().isBefore(LocalDateTime.now())) {
            gb.setStatus("expired");
            groupBuyRepository.save(gb);
            return ApiResponse.fail(400, "该团已过期");
        }

        // 检查是否已参团
        List<Long> members = JSON.parseArray(gb.getMemberIds(), Long.class);
        if (members == null) members = new ArrayList<>();
        if (members.contains(userId)) return ApiResponse.fail(400, "你已参与该团");

        members.add(userId);
        gb.setMemberIds(JSON.toJSONString(members));
        gb.setCurrentCount(members.size());

        // 满人成团：为每个团员自动创建订单
        if (gb.getCurrentCount() >= gb.getRequiredCount()) {
            gb.setStatus("completed");
            for (Long memberId : members) {
                createGroupOrder(memberId, gb);
            }
        }
        groupBuyRepository.save(gb);
        return ApiResponse.ok(toMap(gb));
    }

    /** 所有活跃拼团列表 */
    @GetMapping("/active")
    public ApiResponse<?> active() {
        List<GroupBuy> list = groupBuyRepository.findByStatus("active");
        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupBuy g : list) {
            if (g.getExpiresAt() != null && g.getExpiresAt().isBefore(LocalDateTime.now())) {
                g.setStatus("expired");
                groupBuyRepository.save(g);
                continue;
            }
            result.add(toMap(g));
        }
        return ApiResponse.ok(result);
    }

    private Map<String, Object> toMap(GroupBuy g) {
        Product p = productRepository.findById(g.getProductId()).orElse(null);
        User leader = userRepository.findById(g.getLeaderId()).orElse(null);
        List<Long> members = JSON.parseArray(g.getMemberIds(), Long.class);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("productId", g.getProductId());
        m.put("productTitle", p != null ? p.getTitle() : "");
        m.put("productImage", p != null ? p.getImage() : "");
        m.put("leaderName", leader != null ? leader.getNickname() : "");
        m.put("groupPrice", g.getGroupPrice());
        m.put("requiredCount", g.getRequiredCount());
        m.put("currentCount", g.getCurrentCount());
        m.put("status", g.getStatus());
        m.put("members", members != null ? members : List.of());
        m.put("expiresAt", g.getExpiresAt() != null ? g.getExpiresAt().toString() : null);
        m.put("createdAt", g.getCreatedAt() != null ? g.getCreatedAt().toString() : null);
        return m;
    }

    private void createGroupOrder(Long userId, GroupBuy gb) {
        Product p = productRepository.findById(gb.getProductId()).orElse(null);
        if (p == null) return;
        String orderNo = "GB" + System.currentTimeMillis();
        Order order = Order.builder()
                .orderNo(orderNo)
                .userId(userId)
                .status("pending_payment")
                .total(gb.getGroupPrice())
                .freight(BigDecimal.ZERO)
                .addressSnapshot("{}")
                .build();
        order = orderRepository.save(order);
        OrderItem oi = OrderItem.builder()
                .orderId(order.getId())
                .productId(gb.getProductId())
                .title(p.getTitle())
                .image(p.getImage())
                .price(gb.getGroupPrice())
                .quantity(1)
                .build();
        orderItemRepository.save(oi);
    }
}
