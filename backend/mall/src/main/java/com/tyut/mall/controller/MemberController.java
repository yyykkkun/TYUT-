package com.tyut.mall.controller;

import com.tyut.mall.common.ApiResponse;
import com.tyut.mall.common.UserContext;
import com.tyut.mall.dto.request.AddressRequest;
import com.tyut.mall.dto.request.BrowseHistoryRequest;
import com.tyut.mall.dto.request.ExchangeCouponRequest;
import com.tyut.mall.dto.request.RechargeRequest;
import com.tyut.mall.entity.Favorite;
import com.tyut.mall.entity.Product;
import com.tyut.mall.repository.BalanceRecordRepository;
import com.tyut.mall.repository.FavoriteRepository;
import com.tyut.mall.repository.OrderRepository;
import com.tyut.mall.repository.ProductRepository;
import com.tyut.mall.entity.Order;

import org.springframework.data.domain.PageRequest;
import com.tyut.mall.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final BalanceRecordRepository balanceRecordRepository;
    private final OrderRepository orderRepository;

    @GetMapping("/profile")
    public ApiResponse<?> profile() {
        Long userId = getUserId();
        return ApiResponse.ok(memberService.profile(userId));
    }

    @GetMapping("/addresses")
    public ApiResponse<?> addresses() {
        Long userId = getUserId();
        return ApiResponse.ok(memberService.addresses(userId));
    }

    @PostMapping("/addresses")
    public ApiResponse<?> addAddress(@Valid @RequestBody AddressRequest request) {
        Long userId = getUserId();
        memberService.addAddress(userId, request);
        return ApiResponse.ok();
    }

    @PutMapping("/addresses/{id}/default")
    public ApiResponse<?> setDefaultAddress(@PathVariable Long id) {
        Long userId = getUserId();
        memberService.setDefaultAddress(userId, id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/addresses/{id}")
    public ApiResponse<?> removeAddress(@PathVariable Long id) {
        Long userId = getUserId();
        memberService.removeAddress(userId, id);
        return ApiResponse.ok();
    }

    @GetMapping("/coupons")
    public ApiResponse<?> coupons() {
        Long userId = getUserId();
        return ApiResponse.ok(memberService.coupons(userId));
    }

    @PostMapping("/coupons/exchange")
    public ApiResponse<?> exchangeCoupon(@Valid @RequestBody ExchangeCouponRequest request) {
        Long userId = getUserId();
        boolean result = memberService.exchangeCoupon(userId, request.getPoints());
        return result ? ApiResponse.ok() : ApiResponse.fail(400, "积分不足");
    }

    @GetMapping("/browse-history")
    public ApiResponse<?> browseHistory() {
        Long userId = getUserId();
        return ApiResponse.ok(memberService.browseHistory(userId));
    }

    @PostMapping("/browse-history")
    public ApiResponse<?> addBrowseHistory(@Valid @RequestBody BrowseHistoryRequest request) {
        Long userId = getUserId();
        memberService.addBrowseHistory(userId, request.getProductId());
        return ApiResponse.ok();
    }

    @PutMapping("/avatar")
    public ApiResponse<?> updateAvatar(@RequestBody Map<String, String> body) {
        Long userId = getUserId();
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ApiResponse.fail(400, "头像地址不能为空");
        }
        memberService.updateAvatar(userId, url);
        return ApiResponse.ok();
    }

    // ===== 收藏 =====

    @GetMapping("/favorites")
    public ApiResponse<?> favorites() {
        Long userId = getUserId();
        List<Favorite> list = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Favorite f : list) {
            Product p = productRepository.findById(f.getProductId()).orElse(null);
            if (p == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("productId", f.getProductId());
            m.put("title", p.getTitle());
            m.put("image", p.getImage());
            m.put("price", p.getPrice());
            m.put("createdAt", f.getCreatedAt());
            result.add(m);
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/favorites/{productId}")
    public ApiResponse<?> addFavorite(@PathVariable Long productId) {
        Long userId = getUserId();
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            return ApiResponse.ok(Map.of("favorited", true));
        }
        favoriteRepository.save(Favorite.builder().userId(userId).productId(productId).build());
        return ApiResponse.ok(Map.of("favorited", true));
    }

    @DeleteMapping("/favorites/{productId}")
    public ApiResponse<?> removeFavorite(@PathVariable Long productId) {
        Long userId = getUserId();
        favoriteRepository.deleteByUserIdAndProductId(userId, productId);
        return ApiResponse.ok(Map.of("favorited", false));
    }

    @GetMapping("/favorites/{productId}/status")
    public ApiResponse<?> favoriteStatus(@PathVariable Long productId) {
        Long userId = getUserId();
        if (userId == null) return ApiResponse.ok(Map.of("favorited", false));
        return ApiResponse.ok(Map.of("favorited", favoriteRepository.existsByUserIdAndProductId(userId, productId)));
    }

    @GetMapping("/balance-records")
    public ApiResponse<?> balanceRecords() {
        Long userId = getUserId();
        return ApiResponse.ok(balanceRecordRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/points-records")
    public ApiResponse<?> pointsRecords() {
        Long userId = getUserId();
        List<Map<String, Object>> records = new ArrayList<>();
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 200)).getContent();
        for (Order o : orders) {
            // 下单使用积分
            if (o.getPointsUsed() > 0) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", "po_" + o.getId()); r.put("amount", -o.getPointsUsed());
                r.put("type", "order_use"); r.put("orderNo", o.getOrderNo());
                r.put("remark", "下单抵扣 " + o.getPointsUsed() + " 分（100分=1元）");
                r.put("createdAt", o.getCreatedAt()); records.add(r);
            }
            // 取消退回积分
            if ("cancelled".equals(o.getStatus()) && o.getPointsUsed() > 0) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", "pc_" + o.getId()); r.put("amount", o.getPointsUsed());
                r.put("type", "order_cancel"); r.put("orderNo", o.getOrderNo());
                r.put("remark", "订单取消退回 " + o.getPointsUsed() + " 分");
                r.put("createdAt", o.getUpdatedAt()); records.add(r);
            }
            // 确认收货送积分
            if ("completed".equals(o.getStatus()) && o.getTotal().intValue() > 0) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", "pe_" + o.getId()); r.put("amount", o.getTotal().intValue());
                r.put("type", "points_earn"); r.put("orderNo", o.getOrderNo());
                r.put("remark", "确认收货赠送 " + o.getTotal().intValue() + " 分（1元=1分）");
                r.put("createdAt", o.getUpdatedAt()); records.add(r);
            }
        }
        records.sort((a, b) -> String.valueOf(b.get("createdAt")).compareTo(String.valueOf(a.get("createdAt"))));
        return ApiResponse.ok(records);
    }

    @PostMapping("/recharge")
    public ApiResponse<?> recharge(@Valid @RequestBody RechargeRequest request) {
        Long userId = getUserId();
        memberService.recharge(userId, request.getAmount());
        return ApiResponse.ok(memberService.profile(userId));
    }

    private Long getUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        return userId;
    }
}
