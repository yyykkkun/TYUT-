package com.tyut.mall.repository;

import com.tyut.mall.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findByUserId(Long userId);

    List<UserCoupon> findByUserIdAndUsed(Long userId, Integer used);

    Optional<UserCoupon> findByIdAndUserId(Long id, Long userId);
}
