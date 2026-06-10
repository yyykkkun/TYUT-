package com.tyut.mall.service;

import com.tyut.mall.dto.request.AddressRequest;
import com.tyut.mall.dto.response.AddressVO;
import com.tyut.mall.dto.response.CouponVO;
import com.tyut.mall.dto.response.MemberProfileVO;

import java.util.List;

public interface MemberService {
    MemberProfileVO profile(Long userId);
    List<AddressVO> addresses(Long userId);
    void addAddress(Long userId, AddressRequest request);
    void setDefaultAddress(Long userId, Long addressId);
    void removeAddress(Long userId, Long addressId);
    List<CouponVO> coupons(Long userId);
    boolean exchangeCoupon(Long userId, Integer points);
    List<String> browseHistory(Long userId);
    void addBrowseHistory(Long userId, Long productId);
}
