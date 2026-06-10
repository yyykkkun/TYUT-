package com.tyut.mall.controller;

import com.tyut.mall.common.ApiResponse;
import com.tyut.mall.common.UserContext;
import com.tyut.mall.dto.request.AddressRequest;
import com.tyut.mall.dto.request.BrowseHistoryRequest;
import com.tyut.mall.dto.request.ExchangeCouponRequest;
import com.tyut.mall.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

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

    private Long getUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        return userId;
    }
}
