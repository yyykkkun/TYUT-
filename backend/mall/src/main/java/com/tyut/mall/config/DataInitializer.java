package com.tyut.mall.config;

import com.tyut.mall.entity.*;
import com.tyut.mall.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 初始化演示数据（覆盖 data.sql 中的密码哈希问题）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final UserCouponRepository userCouponRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByUsername("demo@mall.test").isPresent()) {
            // 更新密码为正确的 BCrypt 编码
            User demo = userRepository.findByUsername("demo@mall.test").get();
            String encoded = passwordEncoder.encode("123456");
            if (!passwordEncoder.matches("123456", demo.getPassword())) {
                demo.setPassword(encoded);
                userRepository.save(demo);
                log.info("演示用户密码已更新");
            }
        } else {
            // 创建演示用户
            User demo = User.builder()
                    .username("demo@mall.test")
                    .password(passwordEncoder.encode("123456"))
                    .phone("13800008888")
                    .nickname("演示会员")
                    .level("黄金会员")
                    .balance(BigDecimal.valueOf(688))
                    .points(2680)
                    .giftCard(BigDecimal.valueOf(120))
                    .growth(7420)
                    .status(1)
                    .build();
            demo = userRepository.save(demo);
            log.info("演示用户已创建: demo@mall.test / 123456");

            // 创建地址
            addressRepository.save(Address.builder()
                    .userId(demo.getId()).name("陈同学").phone("13800008888")
                    .province("上海市").city("上海市").district("浦东新区")
                    .detail("世纪大道 100 号 8 栋 1201").isDefault(1).build());
            addressRepository.save(Address.builder()
                    .userId(demo.getId()).name("课程演示").phone("13900006666")
                    .province("江苏省").city("南京市").district("玄武区")
                    .detail("校园路 6 号创新楼").isDefault(0).build());

            // 发放优惠券
            assignCoupon(demo.getId(), 1L, "2026-07-01 00:00:00");
            assignCoupon(demo.getId(), 2L, "2026-06-18 00:00:00");
            assignCoupon(demo.getId(), 3L, "2026-06-30 00:00:00");
        }
    }

    private void assignCoupon(Long userId, Long couponId, String expiresAt) {
        if (userCouponRepository.findByUserId(userId).stream()
                .noneMatch(uc -> uc.getCouponId().equals(couponId))) {
            userCouponRepository.save(UserCoupon.builder()
                    .userId(userId).couponId(couponId).used(0)
                    .expiresAt(LocalDateTime.parse(expiresAt.replace(" ", "T")))
                    .build());
        }
    }
}
