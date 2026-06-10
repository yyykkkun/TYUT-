package com.tyut.mall.service.impl;

import com.tyut.mall.common.ApiException;
import com.tyut.mall.dto.request.AddressRequest;
import com.tyut.mall.dto.response.AddressVO;
import com.tyut.mall.dto.response.CouponVO;
import com.tyut.mall.dto.response.MemberProfileVO;
import com.tyut.mall.entity.*;
import com.tyut.mall.repository.*;
import com.tyut.mall.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final BrowseHistoryRepository browseHistoryRepository;
    private final MessageRepository messageRepository;

    @Override
    public MemberProfileVO profile(Long userId) {
        User user = findUser(userId);
        int couponCount = (int) userCouponRepository.findByUserIdAndUsed(userId, 0).stream().count();
        int unread = messageRepository.countByUserIdAndRead(userId, 0);

        return MemberProfileVO.builder()
                .balance(user.getBalance())
                .points(user.getPoints())
                .giftCard(user.getGiftCard())
                .growth(user.getGrowth())
                .level(user.getLevel())
                .couponCount(couponCount)
                .unreadCount(unread)
                .build();
    }

    @Override
    public List<AddressVO> addresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDesc(userId)
                .stream().map(this::toAddressVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addAddress(Long userId, AddressRequest request) {
        if (request.getIsDefault() != null && request.getIsDefault()) {
            addressRepository.clearDefaultByUserId(userId);
        }
        Address address = Address.builder()
                .userId(userId)
                .name(request.getName())
                .phone(request.getPhone())
                .province(request.getProvince())
                .city(request.getCity())
                .district(request.getDistrict())
                .detail(request.getDetail())
                .isDefault(request.getIsDefault() != null && request.getIsDefault() ? 1 : 0)
                .build();
        addressRepository.save(address);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> ApiException.notFound("地址不存在"));
        if (!address.getUserId().equals(userId)) {
            throw ApiException.badRequest("无权操作");
        }
        addressRepository.clearDefaultByUserId(userId);
        address.setIsDefault(1);
        addressRepository.save(address);
    }

    @Override
    @Transactional
    public void removeAddress(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> ApiException.notFound("地址不存在"));
        if (!address.getUserId().equals(userId)) {
            throw ApiException.badRequest("无权操作");
        }
        addressRepository.delete(address);

        // 如果删的是默认地址，把第一个设为默认
        if (address.getIsDefault() == 1) {
            List<Address> remaining = addressRepository.findByUserIdOrderByIsDefaultDesc(userId);
            if (!remaining.isEmpty()) {
                remaining.get(0).setIsDefault(1);
                addressRepository.save(remaining.get(0));
            }
        }
    }

    @Override
    public List<CouponVO> coupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        return userCoupons.stream().map(uc -> {
            Coupon coupon = couponRepository.findById(uc.getCouponId()).orElse(null);
            return CouponVO.builder()
                    .id(String.valueOf(uc.getId()))
                    .title(coupon != null ? coupon.getTitle() : "未知优惠券")
                    .threshold(coupon != null ? coupon.getThreshold() : null)
                    .amount(coupon != null ? coupon.getAmount() : null)
                    .expiresAt(uc.getExpiresAt() != null ? uc.getExpiresAt().toString() : null)
                    .used(uc.getUsed() == 1)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean exchangeCoupon(Long userId, Integer points) {
        User user = findUser(userId);
        if (user.getPoints() < points) {
            return false;
        }
        // 扣除积分，发放一张10元券
        user.setPoints(user.getPoints() - points);
        userRepository.save(user);

        // 创建优惠券模板（如果没有可复用的话这里简化直接建一个新的）
        Coupon coupon = Coupon.builder()
                .title("积分兑换 10 元券")
                .threshold(BigDecimal.valueOf(60))
                .amount(BigDecimal.valueOf(10))
                .validDays(30)
                .totalCount(1)
                .build();
        coupon = couponRepository.save(coupon);

        UserCoupon uc = UserCoupon.builder()
                .userId(userId)
                .couponId(coupon.getId())
                .used(0)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        userCouponRepository.save(uc);
        return true;
    }

    @Override
    public List<String> browseHistory(Long userId) {
        return browseHistoryRepository.findTop12ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(bh -> String.valueOf(bh.getProductId()))
                .collect(Collectors.toList());
    }

    @Override
    public void addBrowseHistory(Long userId, Long productId) {
        // 先删除已有，再插入（保证去重+置顶）
        browseHistoryRepository.deleteByUserIdAndProductId(userId, productId);
        BrowseHistory history = BrowseHistory.builder()
                .userId(userId)
                .productId(productId)
                .build();
        browseHistoryRepository.save(history);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("用户不存在"));
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
